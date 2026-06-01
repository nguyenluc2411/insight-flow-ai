package com.insightflow.notification.consumer;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.NotificationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationDlqConsumer {

    @KafkaListener(
            topics = NotificationKafkaTopics.OUTGOING_DLQ,
            containerFactory = "notificationCreatedKafkaListenerContainerFactory")
    public void onEmailDlq(
            NotificationCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String failureReason) {

        log.error("[EMAIL-DLQ] topic={} partition={} offset={} eventId={} notificationId={} recipientEmail={} failureReason={}",
                topic,
                partition,
                offset,
                event != null ? event.eventId() : null,
                event != null ? event.notificationId() : null,
                event != null ? event.recipientEmail() : null,
                failureReason);
    }
}

