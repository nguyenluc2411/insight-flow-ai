package com.insightflow.notification.dto.response;

import com.insightflow.notification.enums.DeliveryStatus;
import com.insightflow.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class NotificationDeliveryResponse {
    private UUID id;
    private UUID notificationId;
    private NotificationChannel channel;
    private DeliveryStatus deliveryStatus;
    private int retryCount;
    private String failureReason;
    private Instant deliveredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
