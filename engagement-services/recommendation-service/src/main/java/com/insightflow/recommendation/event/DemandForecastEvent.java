package com.insightflow.recommendation.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DemandForecastEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID productId,
        int forecastDemand,
        double forecastConfidence,
        int forecastPeriodDays
) {
    public static final String TOPIC = "forecast.demand.generated.v1";
    public static final String TYPE = "DemandForecastEvent";
}
