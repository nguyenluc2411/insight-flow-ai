package com.insightflow.integration.service.impl;

import com.insightflow.common.events.integration.OrderSyncedEvent;
import com.insightflow.common.events.integration.ProductSyncedEvent;
import com.insightflow.common.fileparse.DynamicFileParser;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ValidationException;
import com.insightflow.integration.dto.ImportResultDto;
import com.insightflow.integration.dto.ImportRowDto;
import com.insightflow.integration.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * File import → POS-equivalent events.
 *
 * <p>Pipeline: parse (CSV/XLSX) → map+validate rows → dedupe products by SKU →
 * emit {@code integration.product.synced} first (so catalog upserts variants),
 * pause briefly, then {@code integration.order.synced} (resolved by SKU). All
 * synthetic IDs use the {@code FILE} connector type so the data is traceable as
 * file-uploaded rather than POS-synced.
 *
 * <p>Reuses {@link KafkaTemplate} directly (same template/serializer the POS
 * producer uses) rather than {@code IntegrationEventProducer}, whose methods are
 * typed to KiotViet models.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private static final String PRODUCT_TOPIC = "integration.product.synced";
    private static final String ORDER_TOPIC = "integration.order.synced";
    private static final String CONNECTOR_TYPE = "FILE";

    /** If more than this fraction of rows is unusable, reject the whole file. */
    private static final double MAX_SKIP_RATIO = 0.80;
    /** Give catalog time to upsert products before orders arrive (SKU resolution). */
    private static final long INTER_TOPIC_DELAY_MS = 500L;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Accepted normalized header aliases (DynamicFileParser lowercases + strips diacritics).
    private static final List<String> SKU_KEYS =
            List.of("sku", "ma_vach", "ma_san_pham", "ma_hang", "ma_sp", "barcode", "ma");
    private static final List<String> NAME_KEYS =
            List.of("ten_san_pham", "name", "ten_hang", "ten_sp", "product_name", "ten");
    private static final List<String> QTY_KEYS =
            List.of("so_luong_da_ban", "quantity_sold", "so_luong_ban", "sl_ban", "so_luong", "quantity", "qty");
    private static final List<String> DATE_KEYS =
            List.of("ngay_giao_dich", "sale_date", "ngay_ban", "ngay", "order_date", "transaction_date");
    private static final List<String> PRICE_KEYS =
            List.of("gia_ban", "price", "don_gia", "gia", "unit_price", "selling_price");
    private static final List<String> CATEGORY_KEYS =
            List.of("danh_muc", "category", "category_name", "nhom_hang", "loai", "nganh_hang");
    private static final List<String> STOCK_KEYS =
            List.of("ton_kho", "stock", "so_luong_ton", "ton", "stock_quantity", "inventory");

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,           // 2026-05-01
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
    };

    private final DynamicFileParser parser;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public ImportResultDto importFile(UUID tenantId, MultipartFile file) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String lower = fileName.toLowerCase();

        // 1. Format guard — JSON and anything else are not supported yet.
        if (!(lower.endsWith(".csv") || lower.endsWith(".xlsx") || lower.endsWith(".xls"))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Định dạng không hỗ trợ. Chỉ chấp nhận .csv, .xlsx, .xls (JSON chưa được hỗ trợ).");
        }

        // 2. Parse to raw normalized rows.
        List<Map<String, String>> rawRows;
        try (InputStream in = file.getInputStream()) {
            rawRows = parser.parseFile(in, fileName);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.warn("File parse failed tenant={} file={}: {}", tenantId, fileName, e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Không đọc được file: " + e.getMessage());
        }

        int totalRows = rawRows.size();
        if (totalRows == 0) {
            throw new ValidationException("File rỗng hoặc không có dòng dữ liệu.", List.of());
        }

        // 3. Map + validate each row.
        List<ImportRowDto> validRows = new ArrayList<>();
        int skipped = 0;
        for (Map<String, String> row : rawRows) {
            ImportRowDto dto = toRow(row);
            if (dto == null) {
                skipped++;
            } else {
                validRows.add(dto);
            }
        }

        double skipRatio = (double) skipped / totalRows;
        if (validRows.isEmpty() || skipRatio > MAX_SKIP_RATIO) {
            throw new ValidationException(
                    ("Quá nhiều dòng thiếu cột bắt buộc (%d/%d bị bỏ qua). "
                            + "File cần các cột: mã SKU, tên sản phẩm, số lượng đã bán, ngày giao dịch.")
                            .formatted(skipped, totalRows),
                    List.of());
        }

        // 4. Build payloads. Products are deduped by SKU; the product's synthetic
        //    externalId is reused as each order line's externalProductId.
        Map<String, ProductSyncedEvent.SyncedProductPayload> productsBySku = new LinkedHashMap<>();
        List<OrderSyncedEvent.SyncedOrderPayload> orders = new ArrayList<>();
        int orderSeq = 0;
        for (ImportRowDto row : validRows) {
            ProductSyncedEvent.SyncedProductPayload product = productsBySku.computeIfAbsent(
                    row.sku(),
                    sku -> ProductSyncedEvent.SyncedProductPayload.builder()
                            .externalId("FILE_" + UUID.randomUUID())
                            .name(row.name())
                            .sku(sku)
                            .price(row.unitPrice())
                            .stockQuantity(row.stockQuantity())
                            .categoryName(row.categoryName())
                            .build());

            BigDecimal total = row.unitPrice() != null
                    ? row.unitPrice().multiply(BigDecimal.valueOf(row.quantitySold()))
                    : null;

            orders.add(OrderSyncedEvent.SyncedOrderPayload.builder()
                    .externalId("FILE_" + UUID.randomUUID())
                    .orderCode("FILE-ORD-" + (++orderSeq))
                    .customerName(null)
                    .totalAmount(total)
                    .status("completed")
                    .orderedAt(row.saleDate())
                    .lines(List.of(OrderSyncedEvent.SyncedOrderLine.builder()
                            .externalProductId(product.getExternalId())
                            .productCode(row.sku())   // MUST match product SKU for catalog resolution
                            .quantity(row.quantitySold())
                            .unitPrice(row.unitPrice())
                            .build()))
                    .build());
        }

        // 5. Emit product first so catalog upserts variants, then orders.
        String syncJobId = UUID.randomUUID().toString();
        String connectorConfigId = "FILE_IMPORT";

        ProductSyncedEvent productEvent = ProductSyncedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("integration.product.synced")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .connectorType(CONNECTOR_TYPE)
                .connectorConfigId(connectorConfigId)
                .syncJobId(syncJobId)
                .products(new ArrayList<>(productsBySku.values()))
                .build();
        publish(PRODUCT_TOPIC, tenantId.toString(), productEvent);

        sleepBeforeOrders();

        OrderSyncedEvent orderEvent = OrderSyncedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("integration.order.synced")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .connectorType(CONNECTOR_TYPE)
                .connectorConfigId(connectorConfigId)
                .syncJobId(syncJobId)
                .orders(orders)
                .build();
        publish(ORDER_TOPIC, tenantId.toString(), orderEvent);

        log.info("File import tenant={} file={} total={} valid={} skipped={} products={} orders={}",
                tenantId, fileName, totalRows, validRows.size(), skipped, productsBySku.size(), orders.size());

        String message = "Đã tiếp nhận %d dòng (%d sản phẩm). Hệ thống đang đồng bộ dữ liệu, kết quả sẽ hiển thị trong giây lát."
                .formatted(validRows.size(), productsBySku.size());

        return new ImportResultDto(
                syncJobId, fileName, "completed",
                totalRows, validRows.size(), skipped,
                productsBySku.size(), orders.size(), message);
    }

    /**
     * Maps one raw row to a validated {@link ImportRowDto}, or returns {@code null}
     * if any required field is missing/unparseable (caller counts it as skipped).
     */
    private ImportRowDto toRow(Map<String, String> row) {
        String sku = firstNonBlank(row, SKU_KEYS);
        String name = firstNonBlank(row, NAME_KEYS);
        Integer qty = parseQuantity(firstNonBlank(row, QTY_KEYS));
        Instant saleDate = parseDate(firstNonBlank(row, DATE_KEYS));

        if (sku == null || name == null || qty == null || qty <= 0 || saleDate == null) {
            return null;
        }

        BigDecimal price = parsePrice(firstNonBlank(row, PRICE_KEYS));
        String category = firstNonBlank(row, CATEGORY_KEYS);
        Integer stock = parseQuantity(firstNonBlank(row, STOCK_KEYS));

        return new ImportRowDto(sku, name, category, qty, price, stock != null ? stock : 0, saleDate);
    }

    private String firstNonBlank(Map<String, String> row, List<String> keys) {
        for (String key : keys) {
            String v = row.get(key);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private Integer parseQuantity(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        // Keep digits only — handles "150.000 đ", "150,000", "150000".
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant parseDate(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        // ISO datetime first (e.g. "2026-05-01T08:30:00").
        try {
            return LocalDateTime.parse(value).atZone(VN_ZONE).toInstant();
        } catch (Exception ignored) {
            // fall through to date-only formats
        }
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(value, fmt);
                return date.atStartOfDay(VN_ZONE).toInstant();
            } catch (Exception ignored) {
                // try next format
            }
        }
        return null;
    }

    private void sleepBeforeOrders() {
        try {
            Thread.sleep(INTER_TOPIC_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish import event to topic={}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Import event published topic={} offset={}",
                                topic, result.getRecordMetadata().offset());
                    }
                });
    }
}
