package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** Single recommendation item from ml-service */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlRecommendationItem {

    private UUID id;

    @JsonProperty("tenantId")
    private UUID tenantId;

    @JsonProperty("variantId")
    private UUID variantId;

    private String action;
    private String reason;
    private String priority;

    @JsonProperty("suggestedDiscountPct")
    private Double suggestedDiscountPct;

    @JsonProperty("suggestedRestockQty")
    private Integer suggestedRestockQty;

    @JsonProperty("stockAgeDays")
    private Integer stockAgeDays;

    @JsonProperty("currentStock")
    private Integer currentStock;

    @JsonProperty("salesVelocity30d")
    private Double salesVelocity30d;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
