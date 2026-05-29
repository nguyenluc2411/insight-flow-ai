package com.insightflow.notification.dto.kafka;

import com.insightflow.notification.enums.NotificationChannel;
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
public class NotificationFailedEventDto {
    private UUID eventId;
    private UUID notificationId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private UUID recipientId;
    private UUID correlationId;
    private String failureReason;
    private Instant timestamp;
}
