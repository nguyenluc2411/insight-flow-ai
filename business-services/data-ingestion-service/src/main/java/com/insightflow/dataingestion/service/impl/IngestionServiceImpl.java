package com.insightflow.dataingestion.service.impl;

import com.insightflow.dataingestion.client.CatalogClient;
import com.insightflow.dataingestion.dto.event.EventEnvelope;
import com.insightflow.dataingestion.dto.event.InventoryFileUploadedPayload;
import com.insightflow.dataingestion.dto.event.InventoryIngestionFailedPayload;
import com.insightflow.dataingestion.dto.event.InventoryIngestionCompletedPayload;
import com.insightflow.dataingestion.dto.request.EnrichmentRequest;
import com.insightflow.dataingestion.dto.response.EnrichmentResponse;
import com.insightflow.dataingestion.dto.response.WorkspaceInventoryResponse;
import com.insightflow.dataingestion.entity.*;
import com.insightflow.dataingestion.messaging.InventoryEventProducer;
import com.insightflow.dataingestion.repository.*;
import com.insightflow.dataingestion.service.IngestionService;
import com.insightflow.dataingestion.service.S3StorageService;
import com.insightflow.dataingestion.util.DynamicFileParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionServiceImpl implements IngestionService {

    private final IngestionJobRepository ingestionJobRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryFactRepository inventoryFactRepository;
    private final CatalogClient catalogClient;
    private final InventoryEventProducer eventProducer;
    private final S3StorageService s3StorageService;
    private final DynamicFileParser dynamicFileParser;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    @Transactional
    public void handleFileUploadedEvent(EventEnvelope<InventoryFileUploadedPayload> envelope) {
        InventoryFileUploadedPayload payload = envelope.getPayload();
        String tenantId = payload.getTenantId();
        String workspaceId = payload.getWorkspaceId();
        String fileName = payload.getFileName();

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu tenant_id trong sự kiện từ Kafka — không thể ingest an toàn!");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy tên file trong sự kiện từ Kafka!");
        }

        // ✅ 1. SỬA LỖI LOGIC TỰ SÁT: Chỉ check DB trước khi tạo Job mới
        Optional<IngestionJob> existingJobOpt = ingestionJobRepository.findByWorkspaceId(workspaceId);
        if (existingJobOpt.isPresent()) {
            String currentStatus = existingJobOpt.get().getStatus();
            if ("PROCESSING".equals(currentStatus) || "DONE".equals(currentStatus)) {
                log.warn("⚠️ Bỏ qua event. Workspace {} đang được băm data hoặc đã hoàn thành rồi!", workspaceId);
                return;
            }
        }

        // 2. Tiến hành khởi tạo Job và đánh dấu PROCESSING
        IngestionJob job = initOrGetJob(tenantId, workspaceId);

        try {
            log.info("🚀 Bắt đầu luồng UPSERT Data-Driven cho file: {}", fileName);
            String objectKey = "uploads/" + workspaceId + "/" + fileName;

            List<Map<String, String>> parsedRecords;
            try (InputStream s3InputStream = s3StorageService.downloadFileStream(bucketName, objectKey)) {
                parsedRecords = dynamicFileParser.parseFile(s3InputStream, fileName);
            }

            if (parsedRecords.isEmpty()) throw new RuntimeException("File rỗng hoặc sai định dạng!");

            int successCount = 0;
            int totalFieldsChecked = 0;
            int totalFieldsMissing = 0;
            Set<String> allMissingFields = new HashSet<>();

            for (Map<String, String> row : parsedRecords) {
                try {
                    String rawName = row.getOrDefault("ten_san_pham", row.get("name"));
                    String rawCat = row.getOrDefault("danh_muc", row.get("category"));
                    String rawColor = row.getOrDefault("mau_sac", row.get("color"));

                    String productCode = row.getOrDefault("ma_san_pham", row.getOrDefault("product_code", UUID.randomUUID().toString()));
                    String colorSuffix = (rawColor != null && !rawColor.isEmpty()) ? "-" + rawColor : "";
                    String sku = row.getOrDefault("sku", row.getOrDefault("ma_vach", productCode + colorSuffix));

                    List<String> rowMissing = calculateMissingFields(row);
                    allMissingFields.addAll(rowMissing);
                    totalFieldsMissing += rowMissing.size();
                    totalFieldsChecked += 5;

                    EnrichmentRequest request = EnrichmentRequest.builder()
                            .productName(rawName != null ? rawName : "").rawCategory(rawCat != null ? rawCat : "").rawColor(rawColor != null ? rawColor : "").build();
                    EnrichmentResponse aiData = catalogClient.enrichProduct(request);

                    Product product = productRepository.findByTenantIdAndProductCode(tenantId, productCode)
                            .orElseGet(() -> productRepository.save(Product.builder()
                                    .tenantId(tenantId)
                                    .productCode(productCode)
                                    .productName(rawName)
                                    .department(aiData.getDepartment())
                                    .category(aiData.getCategory())
                                    .subCategory(aiData.getSubCategory())
                                    .targetDemographic(aiData.getTargetDemographic())
                                    .material(aiData.getMaterial())
                                    .build()));

                    ProductVariant variant = productVariantRepository.findByTenantIdAndSku(tenantId, sku)
                            .orElseGet(() -> productVariantRepository.save(ProductVariant.builder()
                                    .tenantId(tenantId)
                                    .productId(product.getId())
                                    .sku(sku)
                                    .colorFamily(aiData.getColorFamily())
                                    .colorName(rawColor)
                                    .size(row.getOrDefault("size", row.get("kich_co")))
                                    .build()));

                    InventoryFact fact = inventoryFactRepository.findByVariantIdAndWorkspaceId(variant.getId(), workspaceId)
                            .orElse(InventoryFact.builder()
                                    .tenantId(tenantId)
                                    .variantId(variant.getId())
                                    .workspaceId(workspaceId)
                                    .quantityInStock(0)
                                    .quantitySold(0)
                                    .build());

                    Integer stock = parseInt(row.getOrDefault("ton_kho", row.get("quantity")));
                    if (stock != null) fact.setQuantityInStock(stock);

                    fact.setCostPrice(parseDouble(row.getOrDefault("gia_von", row.get("cost_price"))));
                    fact.setRetailPrice(parseDouble(row.getOrDefault("gia_ban", row.get("retail_price"))));
                    fact.setImportDate(parseDate(row.getOrDefault("ngay_nhap", row.get("import_date"))));

                    inventoryFactRepository.save(fact);
                    successCount++;

                } catch (Exception rowEx) {
                    log.error("Lỗi khi xử lý dòng dữ liệu: {}", row, rowEx);
                }
            }

            double completenessScore = totalFieldsChecked == 0 ? 0.0 :
                    (double) (totalFieldsChecked - totalFieldsMissing) / totalFieldsChecked;

            updateJobStatus(job, parsedRecords.size(), successCount, "DONE", null);

            sendSuccessEvent(tenantId, workspaceId, parsedRecords.size(), completenessScore, new ArrayList<>(allMissingFields));
            log.info("✅ Hoàn tất UPSERT file {}. Thành công: {}. Điểm chất lượng Data: {}", fileName, successCount, completenessScore);

        } catch (Exception ex) {
            log.error("❌ Lỗi sập tiến trình Ingestion cho workspace: " + workspaceId, ex);
            updateJobStatus(job, 0, 0, "ERROR", ex.getMessage());
            sendFailEvent(workspaceId, ex.getMessage());
        }
    }

    @Override
    public WorkspaceInventoryResponse exportWorkspaceData(String tenantId, String workspaceId) {
        List<InventoryFact> facts = inventoryFactRepository.findByTenantIdAndWorkspaceId(tenantId, workspaceId);
        if (facts.isEmpty()) {
            return WorkspaceInventoryResponse.builder()
                    .products(Collections.emptyList())
                    .variants(Collections.emptyList())
                    .inventoryFacts(Collections.emptyList())
                    .build();
        }

        List<String> variantIds = facts.stream().map(InventoryFact::getVariantId).distinct().toList();
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);

        List<String> productIds = variants.stream().map(ProductVariant::getProductId).distinct().toList();
        List<Product> products = productRepository.findAllById(productIds);

        return WorkspaceInventoryResponse.builder()
                .products(products)
                .variants(variants)
                .inventoryFacts(facts)
                .build();
    }

    private IngestionJob initOrGetJob(String tenantId, String workspaceId) {
        IngestionJob job = ingestionJobRepository.findByWorkspaceId(workspaceId).orElse(new IngestionJob());
        job.setId(job.getId() == null ? UUID.randomUUID().toString() : job.getId());
        job.setTenantId(tenantId);
        job.setWorkspaceId(workspaceId);
        job.setStatus("PROCESSING");

        if (job.getTotalRecords() == null) job.setTotalRecords(0);
        if (job.getProcessedRecords() == null) job.setProcessedRecords(0);
        if (job.getFailedRecords() == null) job.setFailedRecords(0);

        job.setStartedAt(OffsetDateTime.now());
        return ingestionJobRepository.save(job);
    }

    private void updateJobStatus(IngestionJob job, int total, int success, String status, String error) {
        job.setTotalRecords(total);
        job.setProcessedRecords(success);
        job.setFailedRecords(total - success);
        job.setStatus(status);
        job.setCompletedAt(OffsetDateTime.now());
        if (error != null) job.setErrorLog("{\"error\":\"" + error.replace("\"", "'") + "\"}");
        ingestionJobRepository.save(job);
    }

    private List<String> calculateMissingFields(Map<String, String> row) {
        List<String> missing = new ArrayList<>();
        if (row.get("gia_von") == null && row.get("cost_price") == null) missing.add("Cost Price");
        if (row.get("gia_ban") == null && row.get("retail_price") == null) missing.add("Retail Price");
        if (row.get("mau_sac") == null && row.get("color") == null) missing.add("Color");
        if (row.get("danh_muc") == null && row.get("category") == null) missing.add("Category");
        if (row.get("ton_kho") == null && row.get("quantity") == null) missing.add("Quantity");
        return missing;
    }

    private void sendSuccessEvent(String tenantId, String workspaceId, int totalItems, double completenessScore, List<String> missingFields) {
        InventoryIngestionCompletedPayload payload = InventoryIngestionCompletedPayload.builder()
                .tenantId(tenantId)
                .workspaceId(workspaceId)
                .totalItems(totalItems)
                .completenessScore(completenessScore)
                .missingFields(missingFields)
                .build();

        EventEnvelope<InventoryIngestionCompletedPayload> env = EventEnvelope.<InventoryIngestionCompletedPayload>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("inventory.ingestion.completed")
                .timestamp(OffsetDateTime.now().toString())
                .source("data-ingestion-service")
                .payload(payload)
                .build();
        eventProducer.sendIngestionCompleted(env);
    }

    private void sendFailEvent(String workspaceId, String errorMsg) {
        InventoryIngestionFailedPayload payload = InventoryIngestionFailedPayload.builder()
                .workspaceId(workspaceId).errorCode("PROCESSING_ERROR").errorMessage(errorMsg).build();

        EventEnvelope<InventoryIngestionFailedPayload> env = EventEnvelope.<InventoryIngestionFailedPayload>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("inventory.ingestion.failed")
                .timestamp(OffsetDateTime.now().toString())
                .source("data-ingestion-service")
                .payload(payload).build();
        eventProducer.sendFailed(env);
    }

    private Double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            log.warn("⚠️ Không thể ép kiểu số thực cho chuỗi: '{}'. Đưa về null", val);
            return null;
        }
    }

    private Integer parseInt(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("⚠️ Không thể ép kiểu số nguyên cho chuỗi: '{}'. Đưa về null", val);
            return null;
        }
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            if (val.contains("/")) {
                return LocalDate.parse(val, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return LocalDate.parse(val);
        } catch (Exception e) {
            log.warn("⚠️ Sai định dạng ngày tháng: '{}'. Đưa về null", val);
            return null;
        }
    }
}