package com.insightflow.notification.dto.request;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {

    // Derived server-side from the authenticated user (@CurrentUser), not the client.
    private UUID userId;

    @NotNull
    private NotificationType notificationType;

    @NotNull
    private NotificationChannel channel;

    @NotNull
    private NotificationSeverity minSeverity;

    private boolean enabled = true;

    private Instant muteUntil;
}
