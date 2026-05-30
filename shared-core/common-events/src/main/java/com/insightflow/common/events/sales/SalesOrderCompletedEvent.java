package com.insightflow.common.events.sales;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insightflow.common.events.EventMetadata;
import com.insightflow.common.events.EventTopics;
import com.insightflow.common.events.InsightFlowEvent;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by {@code sales-service} when an order reaches COMPLETED status.
 * Consumers: {@code catalog-service} (trigger inventory deduction), {@code ml-service} (demand signal)
 * Topic: {@value EventTopics#SALES_ORDER_COMPLETED}
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesOrderCompletedEvent implements InsightFlowEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    String eventType = EventTopics.SALES_ORDER_COMPLETED;

    UUID tenantId;

    @Builder.Default
    Instant occurredAt = Instant.now();

    EventMetadata metadata;

    // --- Domain fields ---

    UUID orderId;
    UUID customerId;
    BigDecimal totalAmount;

    /** ISO-4217 currency code, e.g. {@code "VND"} */
    String currency;

    /** Channel: KIOTVIET, SAPO, HARAVAN, MANUAL */
    String salesChannel;

    List<OrderItemDto> items;
}
