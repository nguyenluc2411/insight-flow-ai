package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Single order item from sales-service */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesOrderItem {

    private UUID id;
    private String status;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
