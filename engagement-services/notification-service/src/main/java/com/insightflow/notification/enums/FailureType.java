package com.insightflow.notification.enums;

import java.util.Arrays;

public enum FailureType {
    RETRYABLE("RETRYABLE"),
    NON_RETRYABLE("NON_RETRYABLE");

    private final String code;

    FailureType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FailureType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("FailureType code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown FailureType code: " + code));
    }
}

