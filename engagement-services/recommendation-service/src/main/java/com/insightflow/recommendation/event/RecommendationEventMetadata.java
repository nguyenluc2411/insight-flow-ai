package com.insightflow.recommendation.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record RecommendationEventMetadata(
        UUID eventId,
        String eventType,
        Instant timestamp
) {
}
