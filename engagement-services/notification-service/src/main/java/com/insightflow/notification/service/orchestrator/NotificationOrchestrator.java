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

import java.util.UUID;

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

        if (event == null) return;

        log.info("[ORCH] START eventId={} correlationId={}",
                event.eventId(),
                event.correlationId());

        boolean first = processedEventService.recordIfNotProcessed(event, "orchestrator");
        if (!first) {
            log.info("[ORCH] DUPLICATE skipped eventId={}", event.eventId());
            return;
        }

        boolean suppressed = aggregationService.tryAggregate(event);
        if (suppressed) {
            log.info("[ORCH] SUPPRESSED eventId={}", event.eventId());
            return;
        }

        Notification notification = notificationMapper.fromIncomingEvent(event);
        notification.setId(UUID.randomUUID());

        Notification saved = notificationRepository.save(notification);

        log.info("[ORCH] SAVED notificationId={} recipientId={}",
                saved.getId(),
                saved.getRecipientId());

        // ================= EMAIL DEBUG =================
        try {
            log.info("[EMAIL] sending notificationId={} to={}",
                    saved.getId(),
                    saved.getRecipientId());

            emailNotificationService.sendEmail(
                    saved
            );

            log.info("[EMAIL] SENT SUCCESS notificationId={}", saved.getId());

        } catch (Exception ex) {
            log.error("[EMAIL] FAILED notificationId={} error={}",
                    saved.getId(),
                    ex.getMessage(),
                    ex);
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

