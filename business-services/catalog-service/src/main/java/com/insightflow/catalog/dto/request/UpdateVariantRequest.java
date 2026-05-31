package com.insightflow.catalog.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateVariantRequest {

    @Size(max = 100)
    private String barcode;

    @Size(max = 20)
    private String size;

    @Size(max = 50)
    private String color;

    @Size(max = 7)
    private String colorHex;

    @Positive
    private BigDecimal costPrice;

    @Positive
    private BigDecimal sellingPrice;

    @Positive
    private BigDecimal compareAtPrice;

    @Size(max = 20)
    private String status;

    // sku is intentionally excluded — it is a business identifier and must not change after creation
}
