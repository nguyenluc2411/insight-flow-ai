package com.insightflow.common.events.catalog;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Emitted by catalog-service after it resolves a POS-synced order
 * (integration.order.synced) against its own product variants. Each line now
 * carries the internal {@code variantId}, so ml-service can ingest it for
 * forecast training without knowing anything about external POS SKUs.
 *
 * Topic: catalog.order.normalized
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CatalogOrderNormalizedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String connectorType;       // "KIOTVIET", "SAPO", ...
    private String externalOrderId;     // POS order id
    private String orderCode;           // POS order code
    private Instant orderedAt;          // original purchase time — the ML time axis
    private List<NormalizedLine> items;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class NormalizedLine {
        private String     variantId;   // resolved internal catalog variant UUID
        private String     sku;
        private Integer    quantity;
        private BigDecimal unitPrice;
    }
}
