package com.insightflow.notification.dto.response;

import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class NotificationAggregationWindowResponse {
    private UUID id;
    private String aggregationKey;
    private NotificationType notificationType;
    private NotificationSeverity severity;
    private Instant windowStart;
    private Instant windowEnd;
    private int aggregatedCount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
