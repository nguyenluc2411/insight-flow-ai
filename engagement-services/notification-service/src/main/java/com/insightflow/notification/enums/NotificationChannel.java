package com.insightflow.notification.enums;

import java.util.Arrays;

public enum NotificationChannel {
    WEBSOCKET("WEBSOCKET", true),
    EMAIL("EMAIL", false),
    PUSH("PUSH", true),
    SMS("SMS", false);

    private final String code;
    private final boolean realtime;

    NotificationChannel(String code, boolean realtime) {
        this.code = code;
        this.realtime = realtime;
    }

    public String getCode() {
        return code;
    }

    public boolean isRealtime() {
        return realtime;
    }

    public static NotificationChannel fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("NotificationChannel code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown NotificationChannel code: " + code));
    }
}

