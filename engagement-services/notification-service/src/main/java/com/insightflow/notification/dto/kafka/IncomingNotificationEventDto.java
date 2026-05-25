package com.insightflow.notification.dto.kafka;

import com.insightflow.notification.enums.NotificationSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomingNotificationEventDto {

    @NotNull
    private UUID eventId;

    @NotBlank
    private String eventType;

    @NotNull
    private Instant timestamp;

    private UUID productId;

    private UUID warehouseId;

    @NotNull
    private NotificationSeverity severity;

    @NotBlank
    private String message;

    private String title;

    private UUID recipientId;

    private UUID correlationId;

    private String sourceService;

    private Map<String, Object> payload;
}
