package com.insightflow.notification.dto.response;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class NotificationTemplateResponse {
    private UUID id;
    private String templateKey;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String subject;
    private String locale;
    private int version;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
