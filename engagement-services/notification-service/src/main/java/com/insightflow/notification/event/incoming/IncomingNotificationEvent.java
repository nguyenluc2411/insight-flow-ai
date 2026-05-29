package com.insightflow.notification.event.incoming;

import com.insightflow.notification.enums.NotificationSeverity;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record IncomingNotificationEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID recipientId,
        NotificationSeverity severity,
        String title,
        String message,
        UUID productId,
        UUID warehouseId,
        UUID correlationId,
        String sourceService,
        Map<String, Object> payload
) {
    public static final String TYPE = "IncomingNotificationEvent";
}
