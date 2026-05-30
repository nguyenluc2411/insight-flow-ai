package com.insightflow.common.events.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insightflow.common.events.EventMetadata;
import com.insightflow.common.events.EventTopics;
import com.insightflow.common.events.InsightFlowEvent;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@code catalog-service} on every inventory movement.
 * Consumers: {@code ml-service} (demand signal), {@code notification-service} (low-stock alerts)
 * Topic: {@value EventTopics#CATALOG_INVENTORY_UPDATED}
 *
 * <p>Inventory uses an append-only {@code inventory_movements} table (event sourcing).
 * This event mirrors each movement row.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatalogInventoryUpdatedEvent implements InsightFlowEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    String eventType = EventTopics.CATALOG_INVENTORY_UPDATED;

    UUID tenantId;

    @Builder.Default
    Instant occurredAt = Instant.now();

    EventMetadata metadata;

    // --- Domain fields ---

    UUID inventoryMovementId;
    UUID productId;
    String productSku;

    /** Location/warehouse identifier */
    UUID locationId;

    int quantityAfter;
    int quantityBefore;

    /** Movement type: SALE, RESTOCK, ADJUSTMENT, RETURN, TRANSFER */
    String movementType;
}
