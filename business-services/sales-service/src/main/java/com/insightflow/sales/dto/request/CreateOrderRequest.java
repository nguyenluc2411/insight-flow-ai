package com.insightflow.sales.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    private UUID locationId;
    private UUID customerId;

    @Size(max = 30)
    private String channel = "pos";

    @Size(max = 30)
    private String paymentMethod;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
}
