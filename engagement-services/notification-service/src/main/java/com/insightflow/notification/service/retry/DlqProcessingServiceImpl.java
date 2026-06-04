package com.insightflow.notification.service.retry;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDlqRecord;
import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.common.events.notification.NotificationDlqEvent;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import com.insightflow.notification.mapper.NotificationKafkaMapper;
import com.insightflow.notification.producer.RetryEventProducer;
import com.insightflow.notification.repository.NotificationDlqRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqProcessingServiceImpl implements DlqProcessingService {

    private final NotificationDlqRecordRepository dlqRecordRepository;
    private final NotificationKafkaMapper kafkaMapper;
    private final RetryEventProducer retryEventProducer;

    @Override
    @Transactional
    public void recordDlq(
            Notification notification,
            NotificationChannel channel,
            String failureReason,
            FailureType failureType,
            int retryCount) {

        NotificationDlqRecord record = new NotificationDlqRecord();
        record.setNotification(notification);
        record.setEventId(notification.getEventId());
        record.setCorrelationId(notification.getCorrelationId());
        record.setRecipientId(notification.getRecipientId());
        record.setChannel(channel);
        record.setEventType(notification.getNotificationType().name());
        record.setFailureReason(failureReason);
        record.setFailureType(failureType);
        record.setRetryCount(retryCount);
        record.setSourceService(notification.getSourceService());
        record.setPayload(notification.getPayload());
        dlqRecordRepository.save(record);

        NotificationDlqEvent dlqEvent = kafkaMapper.toDlqEvent(
                notification,
                channel,
                retryCount,
                failureType,
                failureReason);
        retryEventProducer.publishDlqEvent(dlqEvent);

        log.error("DLQ record persisted topic={} eventId={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={}",
                NotificationKafkaTopics.OUTGOING_DLQ,
                notification.getEventId(),
                notification.getId(),
                retryCount,
                failureType,
                failureReason,
                channel,
                notification.getCorrelationId());
    }

    @Override
    @Transactional
    public void recordDlqMissingNotification(NotificationRetryEvent event, String failureReason) {
        NotificationDlqRecord record = new NotificationDlqRecord();
        record.setEventId(event != null ? event.eventId() : null);
        record.setCorrelationId(event != null ? event.correlationId() : null);
        record.setChannel(event != null && event.channel() != null ? NotificationChannel.fromCode(event.channel()) : null);
        record.setFailureReason(failureReason);
        record.setFailureType(FailureType.NON_RETRYABLE);
        record.setRetryCount(event != null ? event.retryAttempt() : 0);
        record.setPayload(new LinkedHashMap<>());
        dlqRecordRepository.save(record);

        log.error("DLQ record persisted topic={} eventId={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={}",
                NotificationKafkaTopics.OUTGOING_DLQ,
                event != null ? event.eventId() : null,
                event != null ? event.notificationId() : null,
                event != null ? event.retryAttempt() : null,
                FailureType.NON_RETRYABLE,
                failureReason,
                event != null ? event.channel() : null,
                event != null ? event.correlationId() : null);
    }

    @Override
    @Transactional
    public void recordDlqIncomingEvent(IncomingNotificationEvent event, String failureReason) {
        if (event == null) {
            return;
        }
        NotificationDlqRecord record = new NotificationDlqRecord();
        record.setEventId(event.eventId());
        record.setCorrelationId(event.correlationId());
        record.setRecipientId(event.recipientId());
        record.setEventType(event.eventType());
        record.setFailureReason(failureReason);
        record.setFailureType(FailureType.NON_RETRYABLE);
        record.setRetryCount(0);
        record.setSourceService(event.sourceService());
        record.setPayload(buildIncomingPayload(
                event.eventId(),
                event.eventType(),
                event.timestamp(),
                event.recipientId(),
                event.severity(),
                event.title(),
                event.message(),
                event.productId(),
                event.warehouseId(),
                event.correlationId(),
                event.sourceService()));
        dlqRecordRepository.save(record);

        log.error("DLQ record persisted topic={} eventId={} notificationId={} retryCount={} failureType={} failureReason={} channel={} correlationId={}",
                NotificationKafkaTopics.OUTGOING_DLQ,
                event.eventId(),
                null,
                0,
                FailureType.NON_RETRYABLE,
                failureReason,
                null,
                event.correlationId());
    }

    private LinkedHashMap<String, Object> buildIncomingPayload(
            java.util.UUID eventId,
            String eventType,
            java.time.Instant timestamp,
            java.util.UUID recipientId,
            String severity,
            String title,
            String message,
            java.util.UUID productId,
            java.util.UUID warehouseId,
            java.util.UUID correlationId,
            String sourceService) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("timestamp", timestamp);
        payload.put("recipientId", recipientId);
        payload.put("severity", severity);
        payload.put("title", title);
        payload.put("message", message);
        payload.put("productId", productId);
        payload.put("warehouseId", warehouseId);
        payload.put("correlationId", correlationId);
        payload.put("sourceService", sourceService);
        return payload;
    }
}

