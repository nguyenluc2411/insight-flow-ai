package com.insightflow.notification.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

/** Payload for ml.recommendation.created events published by ml-service. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlRecommendationPayload {
    private String eventId;
    private String eventType;
    @JsonProperty("tenantId")
    private UUID tenantId;
    @JsonProperty("variantId")
    private UUID variantId;
    private String action;   // CLEARANCE, RESTOCK, PROMOTE, OK
    private String priority; // HIGH, MEDIUM, LOW
    private String reason;
    @JsonProperty("suggestedDiscountPct")
    private Double suggestedDiscountPct;
    @JsonProperty("stockAgeDays")
    private Integer stockAgeDays;
    @JsonProperty("currentStock")
    private Integer currentStock;
}
