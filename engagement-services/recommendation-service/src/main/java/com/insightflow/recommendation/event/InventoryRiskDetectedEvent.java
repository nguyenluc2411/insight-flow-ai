package com.insightflow.recommendation.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InventoryRiskDetectedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID productId,
        UUID warehouseId,
        int inventoryLevel,
        String riskLevel,
        int stockAgeDays,
        double salesVelocity
) {
    public static final String TOPIC = "inventory.risk.detected.v1";
    public static final String TYPE = "InventoryRiskDetectedEvent";
}

