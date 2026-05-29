package com.insightflow.notification.enums;

import java.util.Arrays;

public enum NotificationStatus {
    PENDING("PENDING", false),
    PROCESSING("PROCESSING", false),
    SENT("SENT", false),
    DELIVERED("DELIVERED", true),
    FAILED("FAILED", true),
    RETRYING("RETRYING", false),
    EXPIRED("EXPIRED", true);

    private final String code;
    private final boolean terminal;

    NotificationStatus(String code, boolean terminal) {
        this.code = code;
        this.terminal = terminal;
    }

    public String getCode() {
        return code;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static NotificationStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("NotificationStatus code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown NotificationStatus code: " + code));
    }
}
