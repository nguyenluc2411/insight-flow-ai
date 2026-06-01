package com.insightflow.notification.service.retry;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDeliveryHistory;
import com.insightflow.notification.entity.NotificationRetry;
import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.DeliveryStatus;
import com.insightflow.notification.enums.NotificationStatus;
import com.insightflow.notification.enums.RetryStatus;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import com.insightflow.notification.exception.ResourceNotFoundException;
import com.insightflow.notification.mapper.NotificationKafkaMapper;
import com.insightflow.notification.producer.KafkaEventPublisher;
import com.insightflow.notification.producer.RetryEventProducer;
import com.insightflow.notification.repository.NotificationDeliveryHistoryRepository;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.repository.NotificationRetryRepository;
import com.insightflow.notification.service.email.EmailNotificationService;
import com.insightflow.notification.service.websocket.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryOrchestratorImpl implements RetryOrchestrator {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationRetryRepository retryRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryHistoryRepository deliveryHistoryRepository;
    private final FailureClassificationService failureClassificationService;
    private final RetryEventProducer retryEventProducer;
    private final KafkaEventPublisher kafkaPublisher;
    private final NotificationKafkaMapper kafkaMapper;
    private final DlqProcessingService dlqProcessingService;
    private final EmailNotificationService emailNotificationService;
    private final RealtimeNotificationService realtimeNotificationService;

    @Override
    @Transactional
    public void handleDeliveryFailure(
            Notification notification,
            NotificationChannel channel,
            String failureReason,
            Throwable exception,
            NotificationDeliveryHistory deliveryHistory) {

        if (notification == null || channel == null) {
            return;
        }

        FailureType failureType = failureClassificationService.classify(exception);
        NotificationRetry retry = resolveLatestRetry(notification.getId(), channel).orElse(new NotificationRetry());
        int nextAttempt = retry.getRetryAttempt() > 0 ? retry.getRetryAttempt() + 1 : 1;

        updateDeliveryHistory(deliveryHistory, failureReason, failureType);

        if (failureType == FailureType.NON_RETRYABLE || nextAttempt > MAX_ATTEMPTS) {
            markRetryExhausted(notification, retry, channel, failureReason, failureType, nextAttempt - 1);
            return;
        }

        Instant nextRetryAt = Instant.now().plus(resolveDelay(nextAttempt));
        NotificationRetry persisted = scheduleRetry(notification, retry, channel, nextAttempt, nextRetryAt, failureReason, failureType);

        notification.setStatus(NotificationStatus.RETRYING);
        notificationRepository.save(notification);

        NotificationRetryEvent retryEvent = kafkaMapper.toRetryEvent(
                notification,
                channel,
                persisted.getRetryAttempt(),
                persisted.getNextRetryAt(),
                failureReason);
        kafkaPublisher.publish(resolveRetryTopic(persisted.getRetryAttempt()), notification.getId().toString(), retryEvent);
        retryEventProducer.publishRetryEvent(retryEvent);

        log.info("Retry scheduled topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                resolveRetryTopic(persisted.getRetryAttempt()),
                notification.getId(),
                persisted.getRetryAttempt(),
                failureType,
                failureReason,
                channel,
                notification.getCorrelationId(),
                notification.getEventId());
    }

    @Override
    @Transactional
    public void handleRetryEvent(NotificationRetryEvent event) {
        if (event == null || event.notificationId() == null) {
            return;
        }

        Notification notification = notificationRepository.findById(event.notificationId()).orElse(null);
        if (notification == null) {
            // Defensive: If notification is missing, record DLQ metadata and return instead of throwing
            dlqProcessingService.recordDlqMissingNotification(event, "Notification not found for retry");
            log.warn("Retry event for missing notification recorded to DLQ eventId={} notificationId={} correlationId={}", event.eventId(), event.notificationId(), event.correlationId());
            return;
        }

        NotificationChannel channel = event.channel() != null
                ? NotificationChannel.fromCode(event.channel())
                : NotificationChannel.EMAIL;

        NotificationRetry retry = resolveLatestRetry(notification.getId(), channel)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationRetry", notification.getId()));

        if (retry.getRetryStatus() == RetryStatus.SUCCEEDED
                || retry.getRetryStatus() == RetryStatus.EXHAUSTED
                || retry.getRetryStatus() == RetryStatus.IN_PROGRESS) {
            log.info("Retry event ignored topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                    resolveRetryTopic(event.retryAttempt()),
                    notification.getId(),
                    retry.getRetryAttempt(),
                    retry.getFailureType(),
                    event.failureReason(),
                    channel,
                    notification.getCorrelationId(),
                    event.eventId());
            return;
        }

        retry.setRetryStatus(RetryStatus.IN_PROGRESS);
        retry.setLastTriedAt(Instant.now());
        retryRepository.save(retry);

        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);

        log.info("Retry attempt started topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                resolveRetryTopic(event.retryAttempt()),
                notification.getId(),
                event.retryAttempt(),
                retry.getFailureType(),
                event.failureReason(),
                channel,
                notification.getCorrelationId(),
                event.eventId());

        switch (channel) {
            case EMAIL -> emailNotificationService.sendEmail(notification);
            case WEBSOCKET -> realtimeNotificationService.pushNotification(notification);
            default -> handleDeliveryFailure(notification, channel,
                    "Unsupported channel for retry: " + channel, null, null);
        }
    }

    @Override
    @Transactional
    public void markRetrySuccess(Notification notification, NotificationChannel channel) {
        if (notification == null || channel == null) {
            return;
        }
        resolveLatestRetry(notification.getId(), channel).ifPresent(retry -> {
            retry.setRetryStatus(RetryStatus.SUCCEEDED);
            retryRepository.save(retry);
            log.info("Retry succeeded topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                    resolveRetryTopic(retry.getRetryAttempt()),
                    notification.getId(),
                    retry.getRetryAttempt(),
                    retry.getFailureType(),
                    retry.getLastFailureReason(),
                    channel,
                    notification.getCorrelationId(),
                    notification.getEventId());
        });
    }

    @Override
    @Transactional
    public void replayFromDlq(Notification notification, NotificationChannel channel, String failureReason) {
        if (notification == null || channel == null) {
            return;
        }
        if (retryRepository.existsByNotification_IdAndRetryStatusIn(
                notification.getId(),
                List.of(RetryStatus.SCHEDULED, RetryStatus.IN_PROGRESS))) {
            log.info("DLQ replay skipped topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                    resolveRetryTopic(1),
                    notification.getId(),
                    1,
                    FailureType.RETRYABLE,
                    failureReason,
                    channel,
                    notification.getCorrelationId(),
                    notification.getEventId());
            return;
        }
        NotificationRetry retry = new NotificationRetry();
        retry.setNotification(notification);
        retry.setChannel(channel);
        retry.setRetryAttempt(1);
        retry.setRetryStatus(RetryStatus.SCHEDULED);
        retry.setLastFailureReason(failureReason);
        retry.setFailureType(FailureType.RETRYABLE);
        retry.setNextRetryAt(Instant.now().plus(resolveDelay(1)));
        retryRepository.save(retry);

        notification.setStatus(NotificationStatus.RETRYING);
        notificationRepository.save(notification);

        NotificationRetryEvent retryEvent = kafkaMapper.toRetryEvent(
                notification,
                channel,
                retry.getRetryAttempt(),
                retry.getNextRetryAt(),
                failureReason);
        kafkaPublisher.publish(resolveRetryTopic(retry.getRetryAttempt()), notification.getId().toString(), retryEvent);
        retryEventProducer.publishRetryEvent(retryEvent);

        log.info("DLQ replay scheduled topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                resolveRetryTopic(retry.getRetryAttempt()),
                notification.getId(),
                retry.getRetryAttempt(),
                FailureType.RETRYABLE,
                failureReason,
                channel,
                notification.getCorrelationId(),
                notification.getEventId());
    }

    private Optional<NotificationRetry> resolveLatestRetry(UUID notificationId, NotificationChannel channel) {
        return retryRepository.findFirstByNotification_IdAndChannelAndRetryStatusInOrderByUpdatedAtDesc(
                notificationId,
                channel,
                List.of(RetryStatus.SCHEDULED, RetryStatus.IN_PROGRESS, RetryStatus.FAILED, RetryStatus.EXHAUSTED, RetryStatus.SUCCEEDED));
    }

    private NotificationRetry scheduleRetry(
            Notification notification,
            NotificationRetry retry,
            NotificationChannel channel,
            int attempt,
            Instant nextRetryAt,
            String failureReason,
            FailureType failureType) {

        retry.setNotification(notification);
        retry.setChannel(channel);
        retry.setRetryAttempt(attempt);
        retry.setRetryStatus(RetryStatus.SCHEDULED);
        retry.setLastFailureReason(failureReason);
        retry.setFailureType(failureType);
        retry.setNextRetryAt(nextRetryAt);
        retry.setLastTriedAt(Instant.now());
        return retryRepository.save(retry);
    }

    private void updateDeliveryHistory(
            NotificationDeliveryHistory history,
            String failureReason,
            FailureType failureType) {
        if (history == null) {
            return;
        }
        history.setDeliveryStatus(DeliveryStatus.FAILED);
        history.setFailureReason(failureReason);
        history.setFailureType(failureType);
        history.setRetryCount(history.getRetryCount() + 1);
        deliveryHistoryRepository.save(history);
    }

    private void markRetryExhausted(
            Notification notification,
            NotificationRetry retry,
            NotificationChannel channel,
            String failureReason,
            FailureType failureType,
            int retryCount) {

        if (retry.getId() == null) {
            retry.setNotification(notification);
            retry.setChannel(channel);
        }
        retry.setRetryStatus(RetryStatus.EXHAUSTED);
        retry.setRetryAttempt(retryCount);
        retry.setLastFailureReason(failureReason);
        retry.setFailureType(failureType);
        retry.setLastTriedAt(Instant.now());
        retryRepository.save(retry);

        notification.setStatus(NotificationStatus.FAILED);
        notificationRepository.save(notification);

        dlqProcessingService.recordDlq(notification, channel, failureReason, failureType, retryCount);

        var failedEvent = kafkaMapper.toFailedEvent(notification, channel, failureReason);
        retryEventProducer.publishFailedEvent(failedEvent);

        log.info("Retry exhausted topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                NotificationKafkaTopics.DLQ,
                notification.getId(),
                retryCount,
                failureType,
                failureReason,
                channel,
                notification.getCorrelationId(),
                notification.getEventId());
    }

    private java.time.Duration resolveDelay(int attempt) {
        if (attempt <= 1) {
            return java.time.Duration.ofSeconds(30);
        }
        if (attempt == 2) {
            return java.time.Duration.ofMinutes(2);
        }
        return java.time.Duration.ofMinutes(10);
    }

    private String resolveRetryTopic(int attempt) {
        if (attempt <= 1) {
            return NotificationKafkaTopics.RETRY_30S;
        }
        if (attempt == 2) {
            return NotificationKafkaTopics.RETRY_2M;
        }
        if (attempt == 3) {
            return NotificationKafkaTopics.RETRY_10M;
        }
        return NotificationKafkaTopics.DLQ;
    }
}

