package com.insightflow.dataingestion.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Object payload) {
        // Gửi không đồng bộ (Async) để không làm block Scheduler
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("❌ [KAFKA ERROR] Failed to publish to topic [{}]. Reason: {}", topic, ex.getMessage());
                // Enterprise Note: Trong thực tế có thể nối thêm Alerting (bắn Slack/Email) ở đây nếu cần.
            } else {
                log.debug("✅ [KAFKA SUCCESS] Published to topic [{}] partition [{}]",
                        topic, result.getRecordMetadata().partition());
            }
        });
    }
}