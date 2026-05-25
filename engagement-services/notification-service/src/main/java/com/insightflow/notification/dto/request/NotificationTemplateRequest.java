package com.insightflow.notification.dto.request;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {

    @NotBlank
    @Size(max = 100)
    private String templateKey;

    @NotNull
    private NotificationType notificationType;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    @Size(max = 255)
    private String subject;

    @NotBlank
    private String body;

    private String htmlBody;

    @Size(max = 10)
    private String locale;

    private boolean active = true;

    private int version = 1;
}
