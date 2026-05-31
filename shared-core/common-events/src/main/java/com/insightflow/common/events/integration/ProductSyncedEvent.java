package com.insightflow.common.events.integration;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductSyncedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String connectorType;       // "KIOTVIET", "SAPO", "HARAVAN"
    private String connectorConfigId;
    private String syncJobId;
    private List<SyncedProductPayload> products;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SyncedProductPayload {
        private String     externalId;
        private String     name;
        private String     sku;
        private BigDecimal price;
        private int        stockQuantity;
        private String     categoryName;  // nullable
    }
}
