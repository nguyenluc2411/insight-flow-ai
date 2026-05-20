package com.insightflow.recommendation.event;

import com.insightflow.recommendation.enums.RecommendationPriority;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record RestockRecommendationEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID recommendationId,
        UUID productId,
        UUID warehouseId,
        RecommendationPriority priority,
        BigDecimal confidenceScore,
        String recommendationReason,
        Map<String, Object> actions
) {
    public static final String TOPIC = "recommendation.restock.v1";
    public static final String TYPE = "RestockRecommendationEvent";
}
