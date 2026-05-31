package com.insightflow.notification.dto.request;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationSeverity;
import lombok.Data;

@Data
public class NotificationInboxFilterRequest {
    private Boolean unread;
    private Boolean archived;
    private NotificationSeverity severity;
    private NotificationChannel channel;
}

