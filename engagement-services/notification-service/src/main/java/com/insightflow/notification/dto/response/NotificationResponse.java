package com.insightflow.notification.dto.response;

import com.insightflow.notification.entity.Notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID tenantId,
        String type,
        String channel,
        String title,
        String body,
        Map<String, Object> metadata,
        boolean isRead,
        Instant sentAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTenantId(),
                n.getType(), n.getChannel(),
                n.getTitle(), n.getBody(),
                n.getMetadata(), n.isRead(),
                n.getSentAt(), n.getCreatedAt()
        );
    }
}
