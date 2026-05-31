package com.insightflow.notification.enums;

import java.util.Arrays;

public enum NotificationType {
    INVENTORY_RISK_ALERT("INVENTORY_RISK_ALERT", "Inventory risk alert"),
    TREND_SPIKE_ALERT("TREND_SPIKE_ALERT", "Trend spike alert"),
    CLEARANCE_RECOMMENDATION("CLEARANCE_RECOMMENDATION", "Clearance recommendation"),
    RESTOCK_RECOMMENDATION("RESTOCK_RECOMMENDATION", "Restock recommendation"),
    DASHBOARD_ALERT("DASHBOARD_ALERT", "Dashboard alert"),
    SYSTEM_WARNING("SYSTEM_WARNING", "System warning"),
    CUSTOM("CUSTOM", "Custom notification");

    private final String code;
    private final String description;

    NotificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("NotificationType code is null");
        }
        if ("RESTOCK_ALERT".equalsIgnoreCase(code)) {
            return RESTOCK_RECOMMENDATION;
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown NotificationType code: " + code));
    }
}

