package com.insightflow.dataingestion.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.dataingestion.client.CatalogClient;
import com.insightflow.dataingestion.dto.event.EventEnvelope;
import com.insightflow.dataingestion.dto.event.InventoryFileUploadedPayload;
import com.insightflow.dataingestion.dto.event.InventoryIngestionFailedPayload;
import com.insightflow.dataingestion.dto.event.InventoryIngestionCompletedPayload;
import com.insightflow.dataingestion.dto.request.ColumnResolveRequest;
import com.insightflow.dataingestion.dto.request.EnrichmentRequest;
import com.insightflow.dataingestion.dto.response.EnrichmentResponse;
import com.insightflow.dataingestion.dto.response.WorkspaceInventoryResponse;
import com.insightflow.dataingestion.entity.*;
import com.insightflow.dataingestion.repository.*;
import com.insightflow.dataingestion.service.IngestionService;
import com.insightflow.dataingestion.service.S3StorageService;
import com.insightflow.common.fileparse.DynamicFileParser;

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
    private final S3StorageService s3StorageService;
    private final DynamicFileParser dynamicFileParser;

    // Đã thay thế InventoryEventProducer bằng Outbox Architecture
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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

            // Phân giải tên cột đa dạng của người dùng -> trường chuẩn (1 lần/file).
            // Lỗi gọi sang product-catalog thì rơi về tên cột mặc định bên dưới, không chặn ingest.
            Map<String, String> keyByField = resolveColumnMapping(parsedRecords);

            int successCount = 0;
            int totalFieldsChecked = 0;
            int totalFieldsMissing = 0;
            Set<String> allMissingFields = new HashSet<>();

            for (Map<String, String> row : parsedRecords) {
                try {
                    String rawName = pick(row, keyByField, "product_name", "ten_san_pham", "name");
                    String rawCat = pick(row, keyByField, "category", "danh_muc", "doanh_muc", "category");
                    String rawColor = pick(row, keyByField, "color", "mau_sac", "color");

                    String productCodeRaw = pick(row, keyByField, "product_code", "ma_san_pham", "product_code");
                    final String productCode = productCodeRaw != null ? productCodeRaw : UUID.randomUUID().toString();
                    String colorSuffix = (rawColor != null && !rawColor.isEmpty()) ? "-" + rawColor : "";
                    String skuRaw = pick(row, keyByField, "sku", "sku", "ma_vach");
                    final String sku = skuRaw != null ? skuRaw : productCode + colorSuffix;

                    List<String> rowMissing = calculateMissingFields(row, keyByField);
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
                                    .size(pick(row, keyByField, "size", "size", "kich_co"))
                                    .build()));

                    InventoryFact fact = inventoryFactRepository.findByVariantIdAndWorkspaceId(variant.getId(), workspaceId)
                            .orElse(InventoryFact.builder()
                                    .tenantId(tenantId)
                                    .variantId(variant.getId())
                                    .workspaceId(workspaceId)
                                    .quantityInStock(0)
                                    .quantitySold(0)
                                    .build());

                    Integer stock = parseInt(pick(row, keyByField, "stock", "ton_kho", "quantity"));
                    if (stock != null) fact.setQuantityInStock(stock);
                    fact.setCostPrice(parseDouble(pick(row, keyByField, "cost_price", "gia_von", "cost_price")));
                    fact.setRetailPrice(parseDouble(pick(row, keyByField, "retail_price", "gia_ban", "retail_price")));
                    fact.setImportDate(parseDate(pick(row, keyByField, "import_date", "ngay_nhap", "import_date")));

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

    private List<String> calculateMissingFields(Map<String, String> row, Map<String, String> keyByField) {
        List<String> missing = new ArrayList<>();
        if (pick(row, keyByField, "cost_price", "gia_von", "cost_price") == null) missing.add("Cost Price");
        if (pick(row, keyByField, "retail_price", "gia_ban", "retail_price") == null) missing.add("Retail Price");
        if (pick(row, keyByField, "color", "mau_sac", "color") == null) missing.add("Color");
        if (pick(row, keyByField, "category", "danh_muc", "doanh_muc", "category") == null) missing.add("Category");
        if (pick(row, keyByField, "stock", "ton_kho", "quantity") == null) missing.add("Quantity");
        return missing;
    }

    /**
     * Lấy giá trị một trường chuẩn từ row: ưu tiên cột đã được product-catalog phân giải,
     * nếu trống thì thử các tên cột mặc định (để file đặt tên cũ vẫn chạy, không phá ngược).
     */
    private String pick(Map<String, String> row, Map<String, String> keyByField,
                        String field, String... legacyKeys) {
        String key = keyByField.get(field);
        if (key != null) {
            String v = row.get(key);
            if (v != null && !v.isEmpty()) return v;
        }
        for (String legacyKey : legacyKeys) {
            String v = row.get(legacyKey);
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    /**
     * Gọi product-catalog phân giải header -> trường chuẩn (1 lần/file), trả về map
     * field -> tên cột thực tế trong row. Lỗi mạng/service thì trả map rỗng để rơi về
     * tên cột mặc định, KHÔNG chặn quá trình ingest.
     */
    private Map<String, String> resolveColumnMapping(List<Map<String, String>> records) {
        Map<String, String> keyByField = new HashMap<>();
        try {
            if (records.isEmpty()) return keyByField;
            List<String> headers = new ArrayList<>(records.get(0).keySet());
            Map<String, String> headerToField = catalogClient.resolveColumns(
                    ColumnResolveRequest.builder().headers(headers).build());
            if (headerToField != null) {
                // Đảo chiều: field -> header (header đầu tiên khớp được giữ lại).
                headerToField.forEach((header, field) -> keyByField.putIfAbsent(field, header));
            }
            log.info("🧭 Map cột động: {} trường nhận diện từ {} cột.", keyByField.size(), headers.size());
        } catch (Exception e) {
            log.warn("⚠️ Không gọi được resolve-columns (dùng tên cột mặc định): {}", e.getMessage());
        }
        return keyByField;
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

        // Convert sang Map và lưu vào Outbox thay vì bắn Kafka trực tiếp
        Map<String, Object> envelopeMap = objectMapper.convertValue(
                env, new TypeReference<Map<String, Object>>() {});

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(UUID.fromString(tenantId))
                .eventType("inventory.ingestion.completed")
                .payload(envelopeMap)
                .build();

        outboxRepository.save(event);
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

        // Convert sang Map và lưu vào Outbox thay vì bắn Kafka trực tiếp
        Map<String, Object> envelopeMap = objectMapper.convertValue(
                env, new TypeReference<Map<String, Object>>() {});

        // Xử lý an toàn: Dùng UUID tạo từ workspaceId để tránh NullPointerException khi tenantId bị thiếu
        UUID safeAggregateId = UUID.nameUUIDFromBytes(workspaceId.getBytes());

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(safeAggregateId)
                .eventType("inventory.ingestion.failed")
                .payload(envelopeMap)
                .build();

        outboxRepository.save(event);
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