package com.insightflow.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFactResponse {
    private String id;
    private String variantId;
    private String workspaceId;
    private String warehouseLocation;

    private Double costPrice;
    private Double retailPrice;
    private Double currentPrice;
    private String currency;

    private Integer quantityInStock;
    private Integer quantitySold;

    private LocalDate importDate;
    private LocalDate lastSoldDate;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}