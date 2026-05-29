package com.insightflow.notification.dto.response;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserNotificationPreferenceResponse {
    private UUID id;
    private UUID userId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private NotificationSeverity minSeverity;
    private boolean enabled;
    private Instant muteUntil;
    private Instant createdAt;
    private Instant updatedAt;
}
