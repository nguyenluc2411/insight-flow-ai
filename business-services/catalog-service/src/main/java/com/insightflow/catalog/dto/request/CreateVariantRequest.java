package com.insightflow.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateVariantRequest {

    @NotBlank
    @Size(max = 150)
    private String sku;

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

    @NotNull
    @Positive
    private BigDecimal sellingPrice;

    @Positive
    private BigDecimal compareAtPrice;

    @Size(max = 20)
    private String status;
}
