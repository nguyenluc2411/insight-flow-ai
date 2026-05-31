package com.insightflow.notification.producer;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.notification.enums.NotificationSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaEventPublisher kafkaEventPublisher;

    public void publishBySeverity(IncomingNotificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }

        String topic = NotificationKafkaTopics.resolvePriorityTopic(NotificationSeverity.fromCode(event.severity()));
        String key = resolveKey(event);
        kafkaEventPublisher.publish(topic, key, event);
    }

    private String resolveKey(IncomingNotificationEvent event) {
        UUID key = event.recipientId() != null ? event.recipientId() : event.eventId();
        return key != null ? key.toString() : null;
    }
}

