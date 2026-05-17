package com.insightflow.sales.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderCompletedEvent(
        UUID eventId,
        String eventType,
        UUID tenantId,
        UUID orderId,
        String orderNumber,
        UUID locationId,
        UUID customerId,
        String channel,
        BigDecimal totalAmount,
        List<OrderItem> items,
        Instant occurredAt
) {
    public static final String TOPIC = "sales.order.completed";
    public static final String TYPE  = "sales.order.completed";

    @Builder
    public record OrderItem(
            UUID variantId,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
