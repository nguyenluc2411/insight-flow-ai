package com.insightflow.notification.service.orchestrator;

import com.insightflow.notification.dto.kafka.IncomingNotificationEventDto;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import com.insightflow.notification.mapper.NotificationKafkaMapper;
import com.insightflow.notification.mapper.NotificationMapper;
import com.insightflow.notification.producer.KafkaEventPublisher;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.aggregation.AggregationService;
import com.insightflow.notification.service.email.EmailNotificationService;
import com.insightflow.notification.service.interfaces.NotificationChannelRouter;
import com.insightflow.notification.service.interfaces.ProcessedEventService;
import com.insightflow.notification.service.websocket.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {

    private final ProcessedEventService processedEventService;
    private final AggregationService aggregationService;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelRouter channelRouter;
    private final EmailNotificationService emailService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final KafkaEventPublisher kafkaPublisher;
    private final NotificationKafkaMapper kafkaMapper;

    @Transactional
    public void orchestrate(IncomingNotificationEventDto event) {
        if (event == null) return;

        log.info("orchestrate: eventId={} correlationId={}", event.getEventId(), event.getCorrelationId());

        IncomingNotificationEvent incomingEvent = IncomingNotificationEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .timestamp(event.getTimestamp())
                .recipientId(event.getRecipientId())
                .severity(event.getSeverity())
                .title(event.getTitle())
                .message(event.getMessage())
                .productId(event.getProductId())
                .warehouseId(event.getWarehouseId())
                .correlationId(event.getCorrelationId())
                .sourceService(event.getSourceService())
                .payload(event.getPayload())
                .build();

        boolean first = processedEventService.recordIfNotProcessed(incomingEvent, "orchestrator");
        if (!first) {
            log.info("Duplicate event skipped eventId={}", event.getEventId());
            return;
        }

        boolean suppressed = aggregationService.tryAggregate(incomingEvent);
        if (suppressed) {
            log.info("Event aggregated/suppressed eventId={}", event.getEventId());
            return;
        }

        // Do NOT assign the id manually: Notification#id is @GeneratedValue, so a
        // pre-set id makes Spring Data treat the entity as existing and issue an
        // UPDATE (ObjectOptimisticLockingFailureException) instead of an INSERT.
        Notification notif = notificationMapper.fromIncomingEvent(event);
        Notification saved = notificationRepository.save(notif);

        List<NotificationChannel> channels = channelRouter.resolveChannels(saved);

        for (NotificationChannel ch : channels) {
            try {
                switch (ch) {
                    case EMAIL:
                        emailService.sendEmail(saved);
                        break;
                    case WEBSOCKET:
                        realtimeNotificationService.pushNotification(saved);
                        break;
                    default:
                        log.warn("Unsupported channel {} for notification {}", ch, saved.getId());
                }
            } catch (Exception ex) {
                log.error("Delivery failure channel={} notificationId={} error={}", ch, saved.getId(), ex.getMessage());
            }
        }

        try {
            var out = kafkaMapper.toBroadcastEvent(saved);
            kafkaPublisher.publish("notifications.outgoing.broadcast", saved.getId().toString(), out);
        } catch (Exception ex) {
            log.error("Failed to publish broadcast event notificationId={} error={}", saved.getId(), ex.getMessage());
        }
    }
}
