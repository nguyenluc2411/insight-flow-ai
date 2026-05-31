package com.insightflow.common.events.notification;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NotificationRetryEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID notificationId,
        String channel,
        int retryAttempt,
        Instant nextRetryAt,
        String failureReason,
        UUID correlationId
) implements NotificationEvent {

    public static final String TYPE = EventType.RETRY.getCode();
}
