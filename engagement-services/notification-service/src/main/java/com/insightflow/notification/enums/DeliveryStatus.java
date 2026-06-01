package com.insightflow.notification.enums;

import java.util.Arrays;

public enum DeliveryStatus {
    PENDING("PENDING", false),
    SENT("SENT", false),
    DELIVERED("DELIVERED", true),
    FAILED("FAILED", true),
    RETRYING("RETRYING", false),
    SKIPPED("SKIPPED", true);

    private final String code;
    private final boolean terminal;

    DeliveryStatus(String code, boolean terminal) {
        this.code = code;
        this.terminal = terminal;
    }

    public String getCode() {
        return code;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static DeliveryStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("DeliveryStatus code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown DeliveryStatus code: " + code));
    }
}

