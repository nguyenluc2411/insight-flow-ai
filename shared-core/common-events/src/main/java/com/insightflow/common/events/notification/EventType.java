package com.insightflow.common.events.notification;

public enum EventType {
    INCOMING("IncomingNotificationEvent"),
    SENT("NotificationSentEvent"),
    RETRY("NotificationRetryEvent"),
    FAILED("NotificationFailedEvent"),
    BROADCAST("NotificationBroadcastEvent"),
    DLQ("NotificationDlqEvent");

    private final String code;

    EventType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
