package com.insightflow.notification.service.orchestrator;

import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.notification.mapper.NotificationKafkaMapper;
import com.insightflow.notification.mapper.NotificationMapper;
import com.insightflow.notification.producer.KafkaEventPublisher;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.aggregation.AggregationService;
import com.insightflow.notification.service.email.EmailNotificationService;
import com.insightflow.notification.service.interfaces.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {

    private final ProcessedEventService processedEventService;
    private final AggregationService aggregationService;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final KafkaEventPublisher kafkaPublisher;
    private final NotificationKafkaMapper kafkaMapper;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public void orchestrate(IncomingNotificationEvent event) {

        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }

        boolean directEmail = event.recipientEmail() != null && !event.recipientEmail().isBlank();

        log.info("[ORCH] START eventId={} correlationId={} recipientEmail={} directEmail={}",
                event.eventId(),
                event.correlationId(),
                event.recipientEmail(),
                directEmail);

        boolean first = processedEventService.recordIfNotProcessed(event, "orchestrator");
        if (!first) {
            log.info("[ORCH] DUPLICATE skipped eventId={}", event.eventId());
            return;
        }

        boolean suppressed = aggregationService.tryAggregate(event);

        log.info("[ORCH] AFTER_AGGREGATION eventId={} directEmail={} suppressed={}",
                event.eventId(),
                directEmail,
                suppressed);

        if (suppressed) {
            log.info("[ORCH] SUPPRESSED eventId={}", event.eventId());
            return;
        }

        Notification notification = notificationMapper.fromIncomingEvent(event);
        if (directEmail && (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank())) {
            notification.setRecipientEmail(event.recipientEmail());
        }

        log.info("[ORCH] MAPPED notificationId={} notificationType={} recipientEmail={}",
                notification.getId(),
                notification.getNotificationType(),
                notification.getRecipientEmail());

        Notification saved;
        try {
            saved = notificationRepository.saveAndFlush(notification);
        } catch (Exception ex) {
            log.error("[ORCH] SAVE_FAILED eventId={} recipientEmail={} error={}",
                    event.eventId(),
                    notification.getRecipientEmail(),
                    ex.getMessage(),
                    ex);
            throw ex;
        }

        log.info("[ORCH] SAVED notificationId={} recipientId={}",
                saved.getId(),
                saved.getRecipientId());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Notification notificationToSend = saved;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("[EMAIL] SENDING notificationId={} to={} recipientEmail={}",
                            notificationToSend.getId(),
                            notificationToSend.getRecipientId(),
                            notificationToSend.getRecipientEmail());
                    emailNotificationService.sendEmail(notificationToSend);
                }
            });
        } else {
            log.info("[EMAIL] SENDING notificationId={} to={} recipientEmail={}",
                    saved.getId(),
                    saved.getRecipientId(),
                    saved.getRecipientEmail());
            emailNotificationService.sendEmail(saved);
        }

        // ================= KAFKA =================
        try {
            kafkaPublisher.publish(
                    NotificationKafkaTopics.OUTGOING_BROADCAST,
                    saved.getId().toString(),
                    kafkaMapper.toBroadcastEvent(saved)
            );

            log.info("[KAFKA] broadcast published notificationId={}", saved.getId());

        } catch (Exception ex) {
            log.error("[KAFKA] broadcast failed notificationId={}", saved.getId(), ex);
        }

        log.info("[ORCH] END notificationId={}", saved.getId());
    }
}

