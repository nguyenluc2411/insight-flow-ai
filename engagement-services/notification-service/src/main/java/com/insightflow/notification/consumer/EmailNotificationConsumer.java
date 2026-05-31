package com.insightflow.notification.consumer;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.NotificationCreatedEvent;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.email.EmailNotificationService;
import com.insightflow.notification.service.interfaces.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final ProcessedEventService processedEventService;
    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;

    @KafkaListener(
            topics = NotificationKafkaTopics.OUTGOING_NOTIFICATION_EVENT,
            containerFactory = "notificationCreatedKafkaListenerContainerFactory")
    public void onNotificationCreated(
            NotificationCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (event == null || event.eventId() == null || event.notificationId() == null) {
            throw new IllegalArgumentException("notification created event, eventId, and notificationId are required");
        }

        boolean first = processedEventService.recordIfNotProcessed(
                event.eventId(),
                event.eventType(),
                event.correlationId(),
                "notification-service:email-consumer",
                event.timestamp(),
                event,
                topic);

        if (!first) {
            log.info("[EMAIL-CONSUMER] Duplicate event ignored topic={} partition={} offset={} eventId={} notificationId={}",
                    topic, partition, offset, event.eventId(), event.notificationId());
            return;
        }

        Notification notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + event.notificationId()));

        if ((notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank())
                && event.recipientEmail() != null
                && !event.recipientEmail().isBlank()) {
            notification.setRecipientEmail(event.recipientEmail());
            notificationRepository.save(notification);
        }

        log.info("[EMAIL-CONSUMER] Processing created event topic={} partition={} offset={} eventId={} notificationId={} recipientEmail={}",
                topic,
                partition,
                offset,
                event.eventId(),
                notification.getId(),
                notification.getRecipientEmail());

        emailNotificationService.sendEmail(notification);
    }
}

