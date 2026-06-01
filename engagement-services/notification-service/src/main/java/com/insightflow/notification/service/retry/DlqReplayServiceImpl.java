package com.insightflow.notification.service.retry;

import com.insightflow.notification.dto.response.DlqReplayResponse;
import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDlqRecord;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.exception.BusinessException;
import com.insightflow.notification.exception.ResourceNotFoundException;
import com.insightflow.notification.repository.NotificationDlqRecordRepository;
import com.insightflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReplayServiceImpl implements DlqReplayService {

    private final NotificationDlqRecordRepository dlqRecordRepository;
    private final NotificationRepository notificationRepository;
    private final RetryOrchestrator retryOrchestrator;

    @Override
    @Transactional
    public DlqReplayResponse replay(UUID notificationId, boolean admin) {
        if (!admin) {
            throw new BusinessException("Admin privileges required", "ADMIN_ONLY", HttpStatus.FORBIDDEN);
        }

        NotificationDlqRecord record = dlqRecordRepository.findFirstByNotification_IdOrderByCreatedAtDesc(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ record", notificationId));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        NotificationChannel channel = record.getChannel() != null ? record.getChannel() : NotificationChannel.EMAIL;
        retryOrchestrator.replayFromDlq(notification, channel, record.getFailureReason());

        log.info("DLQ replay triggered topic={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={} eventId={}",
                NotificationKafkaTopics.RETRY_30S,
                notificationId,
                record.getRetryCount(),
                record.getFailureType(),
                record.getFailureReason(),
                channel,
                notification.getCorrelationId(),
                record.getEventId());

        return new DlqReplayResponse(notificationId, channel, 1, notification.getCorrelationId(), Instant.now());
    }
}

