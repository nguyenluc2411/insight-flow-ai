package com.insightflow.notification.service;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.repository.NotificationPreferenceRepository;
import com.insightflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central dispatcher: checks preferences, persists in-app notification, fires email.
 * Fail-open on email: email failure never prevents in-app notification from being saved.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailSender emailSender;

    private static final List<String> CHANNELS = List.of("IN_APP", "EMAIL");

    @Transactional
    public void dispatch(UUID tenantId, UUID userId,
                         String type, String title, String body,
                         Map<String, Object> metadata) {

        for (String channel : CHANNELS) {
            if (!isChannelEnabled(tenantId, userId, type, channel)) {
                log.debug("Channel {} disabled for tenant={} type={}", channel, tenantId, type);
                continue;
            }

            Notification notification = new Notification();
            notification.setTenantId(tenantId);
            notification.setUserId(userId);
            notification.setType(type);
            notification.setChannel(channel);
            notification.setTitle(title);
            notification.setBody(body);
            notification.setMetadata(metadata);
            notification.setSentAt(Instant.now());

            notificationRepository.save(notification);
            log.debug("Notification persisted tenant={} type={} channel={}", tenantId, type, channel);

            // EMAIL: send asynchronously — fail-open
            if ("EMAIL".equals(channel)) {
                // Phase 1: email address lookup not yet available (no user profile service call).
                // Log intent; wired once tenant email is retrievable from auth-service.
                log.info("Email notification queued for tenant={} type={} — " +
                         "email dispatch requires tenant owner email (phase 2 wiring)", tenantId, type);
            }
        }
    }

    private boolean isChannelEnabled(UUID tenantId, UUID userId, String eventType, String channel) {
        return preferenceRepository
                .findByTenantIdAndEventTypeAndChannel(tenantId, eventType, channel)
                .map(pref -> pref.isEnabled())
                // Default: enabled if no preference record exists
                .orElse(true);
    }
}
