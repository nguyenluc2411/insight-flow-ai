package com.insightflow.common.events.notification;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NotificationDlqEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID notificationId,
        String notificationType,
        String channel,
        UUID recipientId,
        UUID correlationId,
        int retryCount,
        String failureType,
        String failureReason
) implements NotificationEvent {

    public static final String TYPE = EventType.DLQ.getCode();
}
