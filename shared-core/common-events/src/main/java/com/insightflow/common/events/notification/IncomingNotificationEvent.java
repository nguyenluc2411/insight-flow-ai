package com.insightflow.common.events.notification;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record IncomingNotificationEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID recipientId,
        String recipientEmail,
        String severity,
        String title,
        String message,
        UUID productId,
        UUID warehouseId,
        UUID correlationId,
        String sourceService
) implements NotificationEvent {

    public static final String TYPE = EventType.INCOMING.getCode();
}
