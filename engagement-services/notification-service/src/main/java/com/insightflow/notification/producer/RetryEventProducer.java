package com.insightflow.notification.producer;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.NotificationDlqEvent;
import com.insightflow.common.events.notification.NotificationFailedEvent;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryEventProducer {

    private final KafkaEventPublisher kafkaEventPublisher;

    public void publishRetryEvent(NotificationRetryEvent event) {
        if (event == null || event.notificationId() == null) {
            return;
        }
        kafkaEventPublisher.publish(NotificationKafkaTopics.OUTGOING_RETRY, event.notificationId().toString(), event);
        log.info("Published retry event notificationId={} retryAttempt={}", event.notificationId(), event.retryAttempt());
    }

    public void publishFailedEvent(NotificationFailedEvent event) {
        if (event == null || event.notificationId() == null) {
            return;
        }
        kafkaEventPublisher.publish(NotificationKafkaTopics.OUTGOING_FAILED, event.notificationId().toString(), event);
        log.info("Published failed event notificationId={}", event.notificationId());
    }

    public void publishDlqEvent(NotificationDlqEvent event) {
        if (event == null || event.notificationId() == null) {
            return;
        }
        kafkaEventPublisher.publish(NotificationKafkaTopics.OUTGOING_DLQ, event.notificationId().toString(), event);
        log.info("Published DLQ event notificationId={} retryCount={}", event.notificationId(), event.retryCount());
    }
}

