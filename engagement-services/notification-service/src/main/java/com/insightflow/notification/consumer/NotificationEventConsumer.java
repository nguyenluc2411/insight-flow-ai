package com.insightflow.notification.consumer;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.notification.service.interfaces.NotificationEventProcessingService;
import com.insightflow.notification.service.retry.RetryTopicRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationEventProcessingService processingService;
    private final RetryTopicRoutingService retryTopicRoutingService;

    @KafkaListener(
            topics = {
                    NotificationKafkaTopics.HIGH_PRIORITY,
                    NotificationKafkaTopics.NORMAL_PRIORITY,
                    NotificationKafkaTopics.LOW_PRIORITY
            },
            containerFactory = "incomingNotificationKafkaListenerContainerFactory")
    public void onNotificationEvent(
            IncomingNotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        int attempt = retryTopicRoutingService.resolveAttempt(topic);
        log.info("Kafka event received topic={} partition={} offset={} attempt={} eventId={} eventType={} severity={} recipientId={}",
                topic,
                partition,
                offset,
                attempt,
                event != null ? event.eventId() : null,
                event != null ? event.eventType() : null,
                event != null ? event.severity() : null,
                event != null ? event.recipientId() : null);

        processingService.process(event, topic);
        log.info("Kafka event processed topic={} eventId={}",
                topic,
                event != null ? event.eventId() : null);
    }
}

