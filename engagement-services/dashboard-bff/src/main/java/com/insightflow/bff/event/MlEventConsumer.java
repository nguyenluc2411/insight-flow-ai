package com.insightflow.bff.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.common.events.config.EventObjectMapper;
import com.insightflow.common.events.ml.ForecastGeneratedEvent;
import com.insightflow.common.events.ml.RecommendationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Phase 1: logs ml events for observability.
 * Phase 2 will push WebSocket/SSE notifications to connected frontend clients.
 */
@Slf4j
@Component
public class MlEventConsumer {

    private final ObjectMapper objectMapper = EventObjectMapper.create();

    @KafkaListener(
            topics = "ml.forecast.generated",
            groupId = "dashboard-bff-ml-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onForecastGenerated(ConsumerRecord<String, String> record) {
        try {
            ForecastGeneratedEvent event = objectMapper.readValue(record.value(), ForecastGeneratedEvent.class);
            log.info("New forecast available — tenant={} variant={} horizon={}d confidence={} offset={}",
                    event.getTenantId(), event.getVariantId(),
                    event.getForecastHorizon(), event.getConfidence(), record.offset());
        } catch (Exception ex) {
            log.warn("Failed to parse ForecastGeneratedEvent offset={}: {}", record.offset(), ex.getMessage());
        }
    }

    @KafkaListener(
            topics = "ml.recommendation.created",
            groupId = "dashboard-bff-ml-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRecommendationCreated(ConsumerRecord<String, String> record) {
        try {
            RecommendationCreatedEvent event = objectMapper.readValue(record.value(), RecommendationCreatedEvent.class);
            log.info("New recommendation — tenant={} variant={} action={} priority={} offset={}",
                    event.getTenantId(), event.getVariantId(),
                    event.getAction(), event.getPriority(), record.offset());
        } catch (Exception ex) {
            log.warn("Failed to parse RecommendationCreatedEvent offset={}: {}", record.offset(), ex.getMessage());
        }
    }
}
