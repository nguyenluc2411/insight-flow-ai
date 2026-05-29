package com.insightflow.notification.event.outgoing;

import com.insightflow.notification.enums.NotificationChannel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationRetryEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID notificationId,
        NotificationChannel channel,
        int retryAttempt,
        Instant nextRetryAt,
        String failureReason,
        UUID correlationId
) {
    public static final String TYPE = "NotificationRetryEvent";
}
