package com.insightflow.notification.dto.websocket;

import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.enums.NotificationSeverity;
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
public class WebSocketNotificationPayload {
    private UUID notificationId;
    private UUID recipientId;
    private NotificationType type;
    private NotificationSeverity severity;
    private InboxStatus inboxStatus;
    private String title;
    private String message;
    private Map<String, Object> payload;
    private Instant createdAt;
}

