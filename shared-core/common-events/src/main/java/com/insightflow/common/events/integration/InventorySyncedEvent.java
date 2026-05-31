package com.insightflow.common.events.integration;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventorySyncedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String connectorType;
    private String connectorConfigId;
    private String syncJobId;
    private List<SyncedInventoryPayload> inventoryItems;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SyncedInventoryPayload {
        private String externalProductId;
        private String sku;
        private String branchId;
        private int    quantity;
    }
}
