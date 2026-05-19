package com.insightflow.notification.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryUpdatedPayload {
    private String eventId;
    private String eventType;
    private UUID tenantId;
    private UUID variantId;
    private UUID locationId;
    private String movementType;
    private int quantityChange;
    private int newQuantityOnHand;
    @JsonProperty("occurredAt")
    private Instant occurredAt;
}
