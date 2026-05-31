package com.insightflow.notification.service.email;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDeliveryHistory;
import com.insightflow.notification.enums.DeliveryStatus;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.notification.enums.NotificationStatus;
import com.insightflow.notification.mapper.NotificationKafkaMapper;
import com.insightflow.notification.producer.KafkaEventPublisher;
import com.insightflow.notification.provider.email.EmailProvider;
import com.insightflow.notification.repository.NotificationDeliveryHistoryRepository;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.retry.RetryOrchestrator;
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

        // ❌ KHÔNG try/catch ở đây nữa

        String templateKey = notification.getAggregationKey() != null
                ? notification.getAggregationKey()
                : notification.getNotificationType().name();

        var templateOpt = templateService.findActiveTemplate(templateKey);

        Map<String, Object> model = new HashMap<>();
        model.putAll(notification.getPayload() != null ? notification.getPayload() : Map.of());
        model.put("title", notification.getTitle());
        model.put("message", notification.getMessage());

        String body = templateOpt.map(t -> templateService.renderTemplateBody(t, model))
                .orElse(notification.getMessage());

        String html = templateOpt.map(t -> templateService.renderTemplateHtml(t, model))
                .orElse(null);

        // gửi email
        emailProvider.send(notification.getRecipientId(), notification.getTitle(), body, html);

        // update success
        history.setDeliveryStatus(DeliveryStatus.DELIVERED);
        history.setDeliveredAt(Instant.now());
        deliveryRepository.save(history);

        notification.setStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);

        kafkaPublisher.publish(
                NotificationKafkaTopics.OUTGOING_SENT,
                notification.getId().toString(),
                kafkaMapper.toSentEvent(notification, NotificationChannel.EMAIL)
        );

        log.info("Email sent notificationId={} recipient={}",
                notification.getId(),
                notification.getRecipientId());
    }
}

