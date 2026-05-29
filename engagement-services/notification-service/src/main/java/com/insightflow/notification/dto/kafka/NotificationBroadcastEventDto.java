package com.insightflow.notification.dto.kafka;

import com.insightflow.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBroadcastEventDto {
    private UUID eventId;
    private UUID notificationId;
    private NotificationType notificationType;
    private UUID recipientId;
    private UUID correlationId;
    private Instant timestamp;
}
