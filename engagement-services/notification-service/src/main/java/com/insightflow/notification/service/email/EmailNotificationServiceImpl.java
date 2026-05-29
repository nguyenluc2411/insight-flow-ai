package com.insightflow.notification.service.email;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDeliveryHistory;
import com.insightflow.notification.enums.DeliveryStatus;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.mapper.NotificationKafkaMapper;
import com.insightflow.notification.producer.KafkaEventPublisher;
import com.insightflow.notification.provider.email.EmailProvider;
import com.insightflow.notification.repository.NotificationDeliveryHistoryRepository;
import com.insightflow.notification.repository.NotificationRetryRepository;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.template.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final NotificationDeliveryHistoryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService templateService;
    private final EmailProvider emailProvider;
    private final NotificationRetryRepository retryRepository;
    private final NotificationKafkaMapper kafkaMapper;
    private final KafkaEventPublisher kafkaPublisher;

    @Override
    @Async
    @Transactional
    public void sendEmail(Notification notification) {
        if (notification == null) return;

        NotificationDeliveryHistory history = new NotificationDeliveryHistory();
        history.setNotification(notification);
        history.setChannel(NotificationChannel.EMAIL);
        history.setDeliveryStatus(DeliveryStatus.PENDING);
        history.setCreatedAt(Instant.now());
        deliveryRepository.save(history);

        try {
            String templateKey = notification.getAggregationKey() != null ? notification.getAggregationKey() : notification.getNotificationType().name();
            var templateOpt = templateService.findActiveTemplate(templateKey);
            Map<String, Object> model = new HashMap<>();
            model.putAll(notification.getPayload() != null ? notification.getPayload() : Map.of());
            model.put("title", notification.getTitle());
            model.put("message", notification.getMessage());

            String body = templateOpt.map(t -> templateService.renderTemplateBody(t, model)).orElse(notification.getMessage());
            String html = templateOpt.map(t -> templateService.renderTemplateHtml(t, model)).orElse(null);

            emailProvider.send(notification.getRecipientId(), notification.getTitle(), body, html);

            history.setDeliveryStatus(DeliveryStatus.DELIVERED);
            history.setDeliveredAt(Instant.now());
            deliveryRepository.save(history);

            notification.setStatus(com.insightflow.notification.enums.NotificationStatus.SENT);
            notificationRepository.save(notification);

            var sentEvent = kafkaMapper.toSentEvent(notification, NotificationChannel.EMAIL);
            kafkaPublisher.publish("notifications.outgoing.sent", notification.getId().toString(), sentEvent);

            log.info("Email sent notificationId={} recipient={} eventId={}", notification.getId(), notification.getRecipientId(), notification.getEventId());
        } catch (Exception ex) {
            log.error("Email send failed notificationId={} recipient={} error={}", notification.getId(), notification.getRecipientId(), ex.getMessage());
            history.setDeliveryStatus(DeliveryStatus.FAILED);
            history.setFailureReason(ex.getMessage());
            deliveryRepository.save(history);

            var retry = new com.insightflow.notification.entity.NotificationRetry();
            retry.setNotification(notification);
            retry.setChannel(NotificationChannel.EMAIL);
            retry.setRetryAttempt(1);
            retry.setNextRetryAt(Instant.now().plusSeconds(30));
            retryRepository.save(retry);

            notification.setStatus(com.insightflow.notification.enums.NotificationStatus.RETRYING);
            notificationRepository.save(notification);

            var retryEvent = kafkaMapper.toRetryEvent(notification, NotificationChannel.EMAIL, 1, retry.getNextRetryAt(), ex.getMessage());
            kafkaPublisher.publish("notifications.outgoing.retry", notification.getId().toString(), retryEvent);
        }
    }
}
