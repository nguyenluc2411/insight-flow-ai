package com.insightflow.common.events.ml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insightflow.common.events.EventMetadata;
import com.insightflow.common.events.EventTopics;
import com.insightflow.common.events.InsightFlowEvent;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@code ml-service} after a rule-based recommendation is generated.
 * Consumers: {@code dashboard-bff}, {@code notification-service}
 * Topic: {@value EventTopics#ML_RECOMMENDATION_CREATED}
 *
 * <p>Phase 1 recommendation rules (examples):
 * <ul>
 *   <li>Stock &gt; 90 days + sales drop &gt; 50% → CLEARANCE</li>
 *   <li>Stock &lt; 7 days + forecast demand &gt; current → RESTOCK</li>
 *   <li>Slow-moving + seasonal peak approaching → DISCOUNT</li>
 * </ul>
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MlRecommendationCreatedEvent implements InsightFlowEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    String eventType = EventTopics.ML_RECOMMENDATION_CREATED;

    UUID tenantId;

    @Builder.Default
    Instant occurredAt = Instant.now();

    EventMetadata metadata;

    // --- Domain fields ---

    UUID recommendationId;
    UUID productId;
    String productSku;

    /** Action type: CLEARANCE, RESTOCK, DISCOUNT */
    String type;

    /** Urgency level: HIGH, MEDIUM, LOW */
    String urgency;

    double currentStockQuantity;
    double avgDailySales;

    /** Estimated days of stock remaining at current sales rate */
    int daysOfStockRemaining;

    /** Short rule-derived explanation shown to the shop owner */
    String rationale;
}
