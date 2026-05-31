package com.insightflow.notification.config.kafka;

import com.insightflow.notification.enums.NotificationSeverity;

import java.util.List;

public final class NotificationKafkaTopics {

    public static final String HIGH_PRIORITY = "notifications.high";
    public static final String NORMAL_PRIORITY = "notifications.normal";
    public static final String LOW_PRIORITY = "notifications.low";

    public static final String RETRY_30S = "notifications.retry.30s";
    public static final String RETRY_2M = "notifications.retry.2m";
    public static final String RETRY_10M = "notifications.retry.10m";

    public static final String DLQ = "notifications.dlq";

    public static final String OUTGOING_SENT = "notifications.outgoing.sent";
    public static final String OUTGOING_FAILED = "notifications.outgoing.failed";
    public static final String OUTGOING_RETRY = "notifications.outgoing.retry";
    public static final String OUTGOING_BROADCAST = "notifications.outgoing.broadcast";
    public static final String OUTGOING_NOTIFICATION_EVENT = "notification.created";
    public static final String OUTGOING_DLQ = "notifications.outgoing.dlq";

    public static final List<String> PRIORITY_TOPICS = List.of(
            HIGH_PRIORITY,
            NORMAL_PRIORITY,
            LOW_PRIORITY
    );

    public static final List<String> RETRY_TOPICS = List.of(
            RETRY_30S,
            RETRY_2M,
            RETRY_10M
    );

    private NotificationKafkaTopics() {
    }

    public static String resolvePriorityTopic(NotificationSeverity severity) {
        if (severity == null) {
            return NORMAL_PRIORITY;
        }
        return switch (severity) {
            case CRITICAL, HIGH -> HIGH_PRIORITY;
            case MEDIUM -> NORMAL_PRIORITY;
            case LOW -> LOW_PRIORITY;
        };
    }
}

