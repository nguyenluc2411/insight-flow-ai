package com.insightflow.catalog.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InventoryUpdatedEvent(
        UUID eventId,
        String eventType,
        UUID tenantId,
        UUID variantId,
        UUID locationId,
        String movementType,
        int quantityChange,
        int newQuantityOnHand,
        Instant occurredAt
) {
    public static final String TOPIC = "catalog.inventory.updated";
    public static final String TYPE  = "catalog.inventory.updated";
}
