package com.insightflow.recommendation.producer;

import com.insightflow.recommendation.entity.Recommendation;
import com.insightflow.recommendation.event.ClearanceRecommendationEvent;
import com.insightflow.recommendation.event.RecommendationGeneratedEvent;
import com.insightflow.recommendation.event.RestockRecommendationEvent;
import com.insightflow.recommendation.enums.RecommendationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRecommendationEvents(Recommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("recommendation is required");
        }

        publishRecommendationGenerated(recommendation);

        if (recommendation.getRecommendationType() == RecommendationType.CLEARANCE) {
            publishClearanceRecommendation(recommendation);
        }

        if (recommendation.getRecommendationType() == RecommendationType.RESTOCK) {
            publishRestockRecommendation(recommendation);
        }
    }

    public void publishRecommendationGenerated(Recommendation recommendation) {
        RecommendationGeneratedEvent event = RecommendationGeneratedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(RecommendationGeneratedEvent.TYPE)
                .timestamp(Instant.now())
                .recommendationId(recommendation.getId())
                .productId(recommendation.getProductId())
                .warehouseId(recommendation.getWarehouseId())
                .recommendationType(recommendation.getRecommendationType())
                .priority(recommendation.getPriority())
                .riskLevel(recommendation.getRiskLevel())
                .confidenceScore(recommendation.getConfidenceScore())
                .recommendationReason(recommendation.getRecommendationReason())
                .actions(recommendation.getActionPayload())
                .build();

        send(RecommendationGeneratedEvent.TOPIC, recommendation.getProductId(), event);
    }

    public void publishClearanceRecommendation(Recommendation recommendation) {
        ClearanceRecommendationEvent event = ClearanceRecommendationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(ClearanceRecommendationEvent.TYPE)
                .timestamp(Instant.now())
                .recommendationId(recommendation.getId())
                .productId(recommendation.getProductId())
                .warehouseId(recommendation.getWarehouseId())
                .priority(recommendation.getPriority())
                .riskLevel(recommendation.getRiskLevel())
                .confidenceScore(recommendation.getConfidenceScore())
                .recommendationReason(recommendation.getRecommendationReason())
                .actions(recommendation.getActionPayload())
                .build();

        send(ClearanceRecommendationEvent.TOPIC, recommendation.getProductId(), event);
    }

    public void publishRestockRecommendation(Recommendation recommendation) {
        RestockRecommendationEvent event = RestockRecommendationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(RestockRecommendationEvent.TYPE)
                .timestamp(Instant.now())
                .recommendationId(recommendation.getId())
                .productId(recommendation.getProductId())
                .warehouseId(recommendation.getWarehouseId())
                .priority(recommendation.getPriority())
                .confidenceScore(recommendation.getConfidenceScore())
                .recommendationReason(recommendation.getRecommendationReason())
                .actions(recommendation.getActionPayload())
                .build();

        send(RestockRecommendationEvent.TOPIC, recommendation.getProductId(), event);
    }

    private void send(String topic, UUID key, Object event) {
        kafkaTemplate.send(topic, key != null ? key.toString() : null, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event topic={}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Published event topic={} offset={}", topic, result.getRecordMetadata().offset());
                    }
                });
    }
}
