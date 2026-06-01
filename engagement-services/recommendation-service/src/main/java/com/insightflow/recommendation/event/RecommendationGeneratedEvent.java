package com.insightflow.recommendation.event;

import com.insightflow.recommendation.enums.RecommendationPriority;
import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RiskLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record RecommendationGeneratedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID recommendationId,
        UUID productId,
        UUID warehouseId,
        RecommendationType recommendationType,
        RecommendationPriority priority,
        RiskLevel riskLevel,
        BigDecimal confidenceScore,
        String recommendationReason,
        Map<String, Object> actions
) {
    public static final String TOPIC = "recommendation.generated.v1";
    public static final String TYPE = "RecommendationGeneratedEvent";
}
