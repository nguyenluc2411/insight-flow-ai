package com.insightflow.notification.dto.kafka;

import com.insightflow.notification.enums.NotificationChannel;
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
public class NotificationRetryEventDto {
    private UUID eventId;
    private UUID notificationId;
    private NotificationChannel channel;
    private int retryAttempt;
    private Instant nextRetryAt;
    private String failureReason;
    private UUID correlationId;
    private Instant timestamp;
}
