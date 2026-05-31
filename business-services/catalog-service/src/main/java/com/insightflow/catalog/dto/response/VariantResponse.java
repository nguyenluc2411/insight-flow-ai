package com.insightflow.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VariantResponse {
    private UUID id;
    private UUID tenantId;
    private UUID productId;
    private String sku;
    private String barcode;
    private String size;
    private String color;
    private String colorHex;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal compareAtPrice;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
