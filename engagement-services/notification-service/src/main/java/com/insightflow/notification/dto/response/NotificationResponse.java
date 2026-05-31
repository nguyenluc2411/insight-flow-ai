package com.insightflow.notification.dto.response;

import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.enums.NotificationStatus;
import com.insightflow.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID eventId;
    private UUID correlationId;
    private String sourceService;
    private NotificationType notificationType;
    private NotificationSeverity severity;
    private NotificationStatus status;
    private InboxStatus inboxStatus;
    private UUID recipientId;
    private String title;
    private String message;
    private Map<String, Object> payload;
    private String aggregationKey;
    private Instant readAt;
    private Instant archivedAt;
    private Instant deletedAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}

