package com.insightflow.notification.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultKafkaEventPublisher implements KafkaEventPublisher {

    private final KafkaTemplate<String, Object> notificationKafkaTemplate;

    @Override
    public <T> void publish(String topic, String key, T event) {
        notificationKafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed topic={} key={} error={}", topic, key, ex.getMessage());
                    } else if (result != null && result.getRecordMetadata() != null) {
                        log.debug("Kafka publish success topic={} partition={} offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
