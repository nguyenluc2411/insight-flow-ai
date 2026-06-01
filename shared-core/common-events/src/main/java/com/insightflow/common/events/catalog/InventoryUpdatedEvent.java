package com.insightflow.common.events.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /**
     * Category name (raw, shop-defined) — e.g. "Áo Sơ Mi Nam".
     * Nullable when the variant's product has no category.
     * Consumed by ml-service to map → base-model category_key.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String categoryName;

    /**
     * Category slug (raw, URL-friendly) — e.g. "ao-so-mi-nam".
     * Nullable when the variant's product has no category.
     * Consumed by ml-service category_mapper to map → base-model category_key.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String categorySlug;
}
