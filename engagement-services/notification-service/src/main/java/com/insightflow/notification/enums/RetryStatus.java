package com.insightflow.notification.enums;

import java.util.Arrays;

public enum RetryStatus {
    SCHEDULED("SCHEDULED", false),
    IN_PROGRESS("IN_PROGRESS", false),
    SUCCEEDED("SUCCEEDED", true),
    FAILED("FAILED", true),
    EXHAUSTED("EXHAUSTED", true);

    private final String code;
    private final boolean terminal;

    RetryStatus(String code, boolean terminal) {
        this.code = code;
        this.terminal = terminal;
    }

    public String getCode() {
        return code;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static RetryStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("RetryStatus code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RetryStatus code: " + code));
    }
}
