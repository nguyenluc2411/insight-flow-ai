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
public class OrderSyncedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String connectorType;
    private String connectorConfigId;
    private String syncJobId;
    private List<SyncedOrderPayload> orders;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SyncedOrderPayload {
        private String     externalId;
        private String     orderCode;
        private String     customerName;  // nullable
        private BigDecimal totalAmount;
        private String     status;
        private Instant    orderedAt;
    }
}
