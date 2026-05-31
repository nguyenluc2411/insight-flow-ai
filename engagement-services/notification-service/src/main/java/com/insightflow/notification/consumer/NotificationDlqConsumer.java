package com.insightflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationRetry;
import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.repository.NotificationRetryRepository;
import com.insightflow.notification.service.retry.DlqProcessingService;
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

    private final ObjectMapper objectMapper;
    private final DlqProcessingService dlqProcessingService;
    private final NotificationRepository notificationRepository;
    private final NotificationRetryRepository retryRepository;

    @KafkaListener(
            topics = NotificationKafkaTopics.DLQ,
            containerFactory = "retryKafkaListenerContainerFactory")
    public void dlqRetryListener(
            NotificationRetryEvent retryEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String failureReasonHeader) {

        if (retryEvent == null) {
            log.warn("DLQ retry listener received null payload topic={} partition={} offset={}", topic, partition, offset);
            return;
        }
        String failureReason = failureReasonHeader != null ? failureReasonHeader : retryEvent.failureReason();
        handleRetryDlq(topic, partition, offset, retryEvent, failureReason);
    }

    @KafkaListener(
            topics = NotificationKafkaTopics.DLQ,
            containerFactory = "incomingNotificationKafkaListenerContainerFactory")
    public void dlqIncomingListener(
            IncomingNotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String failureReasonHeader) {

        if (event == null) {
            log.warn("DLQ incoming listener received null payload topic={} partition={} offset={}", topic, partition, offset);
            return;
        }

        String failureReason = failureReasonHeader != null ? failureReasonHeader : "DLQ event received for incoming notification: " + event.eventId();

        log.error("Kafka DLQ incoming event topic={} partition={} offset={} eventId={} failureReason={} correlationId={}",
                topic,
                partition,
                offset,
                event.eventId(),
                failureReason,
                event.correlationId());

        dlqProcessingService.recordDlqIncomingEvent(event, failureReason);
    }

    private void handleRetryDlq(
            String topic,
            int partition,
            long offset,
            NotificationRetryEvent retryEvent,
            String failureReason) {

        Notification notification = notificationRepository.findById(retryEvent.notificationId()).orElse(null);
        if (notification == null) {
            log.error("Kafka DLQ retry event missing notification topic={} partition={} offset={} eventId={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={}",
                    topic,
                    partition,
                    offset,
                    retryEvent.eventId(),
                    retryEvent.notificationId(),
                    retryEvent.retryAttempt(),
                    null,
                    failureReason,
                    retryEvent.channel(),
                    retryEvent.correlationId());
            dlqProcessingService.recordDlqMissingNotification(retryEvent, failureReason);
            return;
        }

        NotificationChannel channel = retryEvent.channel() != null
                ? NotificationChannel.fromCode(retryEvent.channel())
                : NotificationChannel.EMAIL;

        NotificationRetry retry = retryRepository.findFirstByNotification_IdAndChannelOrderByUpdatedAtDesc(
                        retryEvent.notificationId(),
                        channel)
                .orElse(null);

        FailureType failureType = retry != null && retry.getFailureType() != null
                ? retry.getFailureType()
                : FailureType.NON_RETRYABLE;
        int retryCount = retry != null ? retry.getRetryAttempt() : retryEvent.retryAttempt();
        log.error("Kafka DLQ retry event topic={} partition={} offset={} eventId={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={}",
                topic,
                partition,
                offset,
                retryEvent.eventId(),
                notification.getId(),
                retryCount,
                failureType,
                failureReason,
                channel,
                notification.getCorrelationId());

        dlqProcessingService.recordDlq(notification, channel, failureReason, failureType, retryCount);
    }
}

