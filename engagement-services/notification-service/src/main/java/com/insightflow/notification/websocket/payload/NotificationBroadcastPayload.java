package com.insightflow.notification.websocket.payload;

import com.insightflow.notification.enums.NotificationSeverity;
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
public class NotificationBroadcastPayload {
    private UUID notificationId;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private Instant timestamp;
}
