package com.insightflow.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryMovementResponse {
    private Long id;
    private UUID variantId;
    private UUID locationId;
    private String movementType;
    private int quantityChange;
    private String referenceType;
    private UUID referenceId;
    private String notes;
    private UUID createdBy;
    private Instant createdAt;
}
