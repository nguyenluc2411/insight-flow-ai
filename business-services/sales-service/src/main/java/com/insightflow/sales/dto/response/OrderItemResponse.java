package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {
    private UUID id;
    private UUID variantId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
}
