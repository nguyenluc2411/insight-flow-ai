package com.insightflow.common.events.sales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OrderCompletedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String         orderId;
    private String         orderNumber;   // e.g. "ORD-abc12345-1716000000000"
    private String         customerId;    // nullable
    private BigDecimal     totalAmount;
    private String         currency;      // "VND"
    private List<OrderItemPayload> items;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OrderItemPayload {
        private String     variantId;
        private String     sku;
        private int        quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
