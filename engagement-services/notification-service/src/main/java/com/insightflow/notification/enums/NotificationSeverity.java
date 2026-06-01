package com.insightflow.notification.enums;

import java.util.Arrays;

public enum NotificationSeverity {
    LOW("LOW", 1, "Low severity"),
    MEDIUM("MEDIUM", 2, "Medium severity"),
    HIGH("HIGH", 3, "High severity"),
    CRITICAL("CRITICAL", 4, "Critical severity");

    private final String code;
    private final int weight;
    private final String description;

    NotificationSeverity(String code, int weight, String description) {
        this.code = code;
        this.weight = weight;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationSeverity fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("NotificationSeverity code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown NotificationSeverity code: " + code));
    }
}

