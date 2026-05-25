package com.insightflow.notification.dto.request;

import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    private UUID eventId;
    private UUID correlationId;

    @NotNull
    private NotificationType notificationType;

    @NotNull
    private NotificationSeverity severity;

    @NotNull
    private UUID recipientId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String message;

    @Size(max = 100)
    private String sourceService;

    @Size(max = 200)
    private String aggregationKey;

    private Map<String, Object> payload;

    private Instant expiresAt;
}
