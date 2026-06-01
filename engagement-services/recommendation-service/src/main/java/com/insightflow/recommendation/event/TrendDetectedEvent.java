package com.insightflow.recommendation.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TrendDetectedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID productId,
        String category,
        double trendScore,
        String trendDirection,
        String region,
        int forecastWindowDays
) {
    public static final String TOPIC = "fashion.trend.detected.v1";
    public static final String TYPE = "TrendDetectedEvent";
}
