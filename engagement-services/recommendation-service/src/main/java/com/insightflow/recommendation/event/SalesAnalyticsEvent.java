package com.insightflow.recommendation.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SalesAnalyticsEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID productId,
        int dailySales,
        double weeklyGrowthRate,
        double salesVelocity,
        double returnRate
) {
    public static final String TOPIC = "sales.analytics.generated.v1";
    public static final String TYPE = "SalesAnalyticsEvent";
}
