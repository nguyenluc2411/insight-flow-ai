package com.insightflow.common.events.integration;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SyncCompletedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String syncJobId;
    private String connectorType;
    private String connectorConfigId;
    private String syncType;        // "FULL", "INCREMENTAL"
    private String status;          // "SUCCESS", "PARTIAL", "FAILED"
    private int    totalProducts;
    private int    totalOrders;
    private int    totalInventory;
    private long   durationMs;
    private String errorMessage;    // nullable
}
