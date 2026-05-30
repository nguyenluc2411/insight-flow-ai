package com.insightflow.common.events.sales;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.UUID;

/** A single line item within a {@link SalesOrderCompletedEvent}. */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemDto {

    UUID productId;
    String productSku;
    int quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
}
