package com.insightflow.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryLevelResponse {
    private UUID id;
    private UUID variantId;
    private UUID locationId;
    private String locationName;
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable;
    private Integer reorderPoint;
    private Instant updatedAt;
}
