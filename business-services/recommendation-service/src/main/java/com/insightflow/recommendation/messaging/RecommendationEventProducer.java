package com.insightflow.recommendation.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.recommendation.dto.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecommendationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper; // ✅ Vũ khí ép kiểu JSON

    public void sendRecommendationGenerated(String workspaceId) {
        try {
            EventEnvelope<Map<String, String>> env = EventEnvelope.<Map<String, String>>builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("inventory.recommendation.generated")
                    .timestamp(OffsetDateTime.now().toString())
                    .source("recommendation-service")
                    .payload(Map.of("workspace_id", workspaceId))
                    .build();

            // ✅ Chuyển Object thành String JSON trước khi gửi
            String jsonMessage = objectMapper.writeValueAsString(env);
            kafkaTemplate.send("inventory.recommendation.generated", workspaceId, jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi chuyển đổi JSON Kafka: " + e.getMessage());
        }
    }

    public void sendRecommendationFailed(String workspaceId, String errorMessage) {
        try {
            EventEnvelope<Map<String, String>> env = EventEnvelope.<Map<String, String>>builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("inventory.recommendation.failed")
                    .timestamp(OffsetDateTime.now().toString())
                    .source("recommendation-service")
                    .payload(Map.of("workspace_id", workspaceId, "error_message", errorMessage))
                    .build();

            // ✅ Chuyển Object thành String JSON trước khi gửi
            String jsonMessage = objectMapper.writeValueAsString(env);
            kafkaTemplate.send("inventory.recommendation.failed", workspaceId, jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi chuyển đổi JSON Kafka: " + e.getMessage());
        }
    }
}