package com.insightflow.common.events.catalog;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryUpdatedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String variantId;
    private String locationId;
    private String movementType;   // "RESTOCK", "SALE", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT"
    private int    quantityChange; // positive = add, negative = subtract
    private int    quantityOnHand; // snapshot after movement
    private String productId;
    private String sku;
    private String referenceType;  // nullable — e.g. "SALES_ORDER"
    private String referenceId;    // nullable — order UUID if applicable
}
