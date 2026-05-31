package com.insightflow.billing.event.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish message to topic [{}]: {}", topic, ex.getMessage());
            } else {
                log.debug("Published message to topic [{}] partition [{}] offset [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
