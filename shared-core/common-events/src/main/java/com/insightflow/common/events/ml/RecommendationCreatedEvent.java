package com.insightflow.common.events.ml;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecommendationCreatedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String  variantId;
    private String  action;             // "CLEARANCE", "RESTOCK", "PROMOTE", "OK"
    private String  priority;           // "HIGH", "MEDIUM", "LOW"
    private String  reason;
    private Integer suggestedDiscount;  // nullable — % discount (CLEARANCE only)
    private Integer stockAgeDays;       // nullable
    private Integer forecastDemand30d;  // nullable
}
