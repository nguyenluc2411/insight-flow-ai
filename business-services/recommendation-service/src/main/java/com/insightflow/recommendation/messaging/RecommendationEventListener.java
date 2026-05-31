package com.insightflow.recommendation.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.recommendation.dto.event.EventEnvelope;
import com.insightflow.recommendation.dto.event.InventoryIngestionCompletedPayload;
import com.insightflow.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEventListener {

    private final ObjectMapper objectMapper;
    private final RecommendationService recommendationService;

    @KafkaListener(topics = "inventory.ingestion.completed", groupId = "ai-recommendation-group")
    public void consumeIngestionCompleted(String payload) {
        try {
            EventEnvelope<InventoryIngestionCompletedPayload> envelope = objectMapper.readValue(payload,
                    new TypeReference<EventEnvelope<InventoryIngestionCompletedPayload>>() {});

            log.info("✅ Nhận lệnh từ Kafka: Kho hàng {} đã nạp xong. Kích hoạt AI...", envelope.getPayload().getWorkspaceId());
            recommendationService.processRecommendation(envelope.getPayload());
        } catch (Exception e) {
            log.error("❌ Lỗi khi parse dữ liệu từ Kafka.", e);
        }
    }
}