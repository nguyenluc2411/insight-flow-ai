package com.insightflow.notification.enums;

import java.util.Arrays;

public enum NotificationType {
    // --- Realtime (business-critical, acted on immediately) ---
    CRITICAL_STOCK_RISK("CRITICAL_STOCK_RISK", "Critical stock risk", DeliveryPolicy.REALTIME),
    INTEGRATION_DISCONNECTED("INTEGRATION_DISCONNECTED", "Integration disconnected", DeliveryPolicy.REALTIME),
    SYNC_FAILED("SYNC_FAILED", "Data sync failed", DeliveryPolicy.REALTIME),
    SYSTEM_WARNING("SYSTEM_WARNING", "System warning", DeliveryPolicy.REALTIME),
    SUBSCRIPTION_EXPIRED("SUBSCRIPTION_EXPIRED", "Subscription expired", DeliveryPolicy.REALTIME),
    CUSTOM("CUSTOM", "Custom notification", DeliveryPolicy.REALTIME),

    // --- Daily report (folded into the single 04:00 inventory report) ---
    DAILY_INVENTORY_REPORT("DAILY_INVENTORY_REPORT", "Daily inventory report", DeliveryPolicy.DAILY_REPORT),

    // --- Legacy types: no longer emitted as standalone notifications. ---
    // Inventory actions now surface as sections of DAILY_INVENTORY_REPORT;
    // forecast/trend are Dashboard-only insight (no inbox record).
    INVENTORY_RISK_ALERT("INVENTORY_RISK_ALERT", "Inventory risk alert", DeliveryPolicy.DAILY_REPORT),
    CLEARANCE_RECOMMENDATION("CLEARANCE_RECOMMENDATION", "Clearance recommendation", DeliveryPolicy.DAILY_REPORT),
    RESTOCK_RECOMMENDATION("RESTOCK_RECOMMENDATION", "Restock recommendation", DeliveryPolicy.DAILY_REPORT),
    TREND_SPIKE_ALERT("TREND_SPIKE_ALERT", "Trend spike alert", DeliveryPolicy.DASHBOARD_NONE),
    DASHBOARD_ALERT("DASHBOARD_ALERT", "Dashboard alert", DeliveryPolicy.DASHBOARD_NONE);

    private final String code;
    private final String description;
    private final DeliveryPolicy deliveryPolicy;

    NotificationType(String code, String description, DeliveryPolicy deliveryPolicy) {
        this.code = code;
        this.description = description;
        this.deliveryPolicy = deliveryPolicy;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DeliveryPolicy getDeliveryPolicy() {
        return deliveryPolicy;
    }

    public static NotificationType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("NotificationType code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown NotificationType code: " + code));
    }
}
