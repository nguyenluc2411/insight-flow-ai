package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SalesOrderResponse {
    private UUID id;
    private UUID tenantId;
    private String orderNumber;
    private UUID locationId;
    private UUID customerId;
    private String channel;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private Instant orderedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponse> items;
}
