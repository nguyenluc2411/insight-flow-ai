package com.insightflow.common.events.ml;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ForecastGeneratedEvent {

    // base fields
    private String eventId;
    private String eventType;
    private String tenantId;
    private Instant occurredAt;

    // domain fields
    private String variantId;
    private int    forecastHorizon;    // number of days, e.g. 30
    private String modelType;          // "prophet", "moving_average"
    private String confidence;         // "high", "medium", "low"
    private int    totalForecastQty;
    private String forecastDate;       // ISO date string, e.g. "2026-05-20"
}
