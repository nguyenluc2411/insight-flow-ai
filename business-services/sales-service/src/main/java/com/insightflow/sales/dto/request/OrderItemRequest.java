package com.insightflow.sales.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemRequest {

    @NotNull
    private UUID variantId;

    @Min(1)
    private int quantity;

    @NotNull
    @Positive
    private BigDecimal unitPrice;

    private BigDecimal unitCost;

    private BigDecimal discountAmount;
}
