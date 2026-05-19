package com.insightflow.bff.event;

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

    @KafkaListener(
            topics = "ml.forecast.generated",
            groupId = "dashboard-bff-ml-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onForecastGenerated(ConsumerRecord<String, String> record) {
        log.info("New forecast available — tenant={} offset={} payload={}",
                extractTenantId(record.value()), record.offset(), truncate(record.value()));
    }

    @KafkaListener(
            topics = "ml.recommendation.created",
            groupId = "dashboard-bff-ml-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRecommendationCreated(ConsumerRecord<String, String> record) {
        log.info("New recommendations available — tenant={} offset={} payload={}",
                extractTenantId(record.value()), record.offset(), truncate(record.value()));
    }

    private String extractTenantId(String json) {
        if (json == null) return "unknown";
        int idx = json.indexOf("\"tenant_id\"");
        if (idx < 0) idx = json.indexOf("\"tenantId\"");
        if (idx < 0) return "unknown";
        int colon = json.indexOf(":", idx);
        int start = json.indexOf("\"", colon) + 1;
        int end = json.indexOf("\"", start);
        return (start > 0 && end > start) ? json.substring(start, end) : "unknown";
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
