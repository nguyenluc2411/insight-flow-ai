package com.insightflow.notification.consumer;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDlqConsumer {

    @KafkaListener(
            topics = NotificationKafkaTopics.DLQ,
            containerFactory = "incomingNotificationKafkaListenerContainerFactory")
    public void onDlqEvent(
            IncomingNotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("Kafka DLQ event topic={} partition={} offset={} eventId={} eventType={} severity={} recipientId={}",
                topic,
                partition,
                offset,
                event != null ? event.eventId() : null,
                event != null ? event.eventType() : null,
                event != null ? event.severity() : null,
                event != null ? event.recipientId() : null);
    }
}
