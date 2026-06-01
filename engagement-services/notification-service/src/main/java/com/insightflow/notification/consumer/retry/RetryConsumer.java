package com.insightflow.notification.consumer.retry;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.RetryStatus;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.entity.NotificationRetry;
import com.insightflow.notification.repository.NotificationRetryRepository;
import com.insightflow.notification.service.retry.RetryOrchestrator;
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
public class RetryConsumer {

    private final RetryOrchestrator retryOrchestrator;
    private final NotificationRepository notificationRepository;
    private final NotificationRetryRepository retryRepository;
    private final com.insightflow.notification.service.retry.DlqProcessingService dlqProcessingService;

    @KafkaListener(
            topics = {
                    NotificationKafkaTopics.RETRY_30S,
                    NotificationKafkaTopics.RETRY_2M,
                    NotificationKafkaTopics.RETRY_10M
            },
            containerFactory = "retryKafkaListenerContainerFactory")
    public void onRetryEvent(
            NotificationRetryEvent retryEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (retryEvent == null || retryEvent.notificationId() == null) {
            log.warn("Retry event skipped topic={} partition={} offset={} notificationId=null retryCount=null failureType=null failureReason=null eventId=null correlationId=null",
                    topic, partition, offset);
            return;
        }

        Notification notification = notificationRepository.findById(retryEvent.notificationId()).orElse(null);
        if (notification == null) {
            log.warn("Retry event notification missing topic={} partition={} offset={} notificationId={} retryCount={} failureType={} failureReason={} eventId={} correlationId={}",
                    topic,
                    partition,
                    offset,
                    retryEvent.notificationId(),
                    retryEvent.retryAttempt(),
                    null,
                    retryEvent.failureReason(),
                    retryEvent.eventId(),
                    retryEvent.correlationId());
            // Record missing notification to DLQ and do not call orchestrator which expects the domain object to exist
            // DlqProcessingService will persist the DLQ metadata for further inspection/replay
            // Inject DlqProcessingService and call recordDlqMissingNotification
            dlqProcessingService.recordDlqMissingNotification(retryEvent, retryEvent.failureReason() != null ? retryEvent.failureReason() : "Notification not found for retry");
            return;
        }

        NotificationChannel channel = retryEvent.channel() != null
                ? NotificationChannel.fromCode(retryEvent.channel())
                : NotificationChannel.EMAIL;

        NotificationRetry retry = retryRepository.findFirstByNotification_IdAndChannelOrderByUpdatedAtDesc(
                notification.getId(),
                channel).orElse(null);
        boolean terminalRetryExists = retry != null
                && (retry.getRetryStatus() == RetryStatus.SUCCEEDED
                || retry.getRetryStatus() == RetryStatus.EXHAUSTED
                || retry.getRetryStatus() == RetryStatus.IN_PROGRESS);
        if (terminalRetryExists) {
            log.info("Retry event ignored topic={} partition={} offset={} notificationId={} retryCount={} failureType={} failureReason={} eventId={} correlationId={}",
                    topic,
                    partition,
                    offset,
                    notification.getId(),
                    retry != null ? retry.getRetryAttempt() : retryEvent.retryAttempt(),
                    retry != null ? retry.getFailureType() : null,
                    retryEvent.failureReason(),
                    retryEvent.eventId(),
                    notification.getCorrelationId());
            return;
        }

        log.info("Retry event received topic={} partition={} offset={} notificationId={} retryCount={} failureType={} failureReason={} eventId={} correlationId={}",
                topic,
                partition,
                offset,
                notification.getId(),
                retry != null ? retry.getRetryAttempt() : retryEvent.retryAttempt(),
                retry != null ? retry.getFailureType() : null,
                retryEvent.failureReason(),
                retryEvent.eventId(),
                notification.getCorrelationId());

        retryOrchestrator.handleRetryEvent(retryEvent);
    }
}

