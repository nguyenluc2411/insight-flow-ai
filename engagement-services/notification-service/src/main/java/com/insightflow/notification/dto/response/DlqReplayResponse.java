package com.insightflow.notification.dto.response;

import com.insightflow.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DlqReplayResponse {
    private UUID notificationId;
    private NotificationChannel channel;
    private int retryAttempt;
    private UUID correlationId;
    private Instant replayedAt;
}

