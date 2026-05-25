package com.insightflow.notification.event.outgoing;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationSentEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID notificationId,
        NotificationType notificationType,
        NotificationChannel channel,
        UUID recipientId,
        UUID correlationId
) {
    public static final String TYPE = "NotificationSentEvent";
}
