package com.insightflow.notification.enums;

import java.util.Arrays;

public enum InboxStatus {
    UNREAD("UNREAD", true),
    READ("READ", true),
    ARCHIVED("ARCHIVED", false),
    DELETED("DELETED", false);

    private final String code;
    private final boolean visibleInInbox;

    InboxStatus(String code, boolean visibleInInbox) {
        this.code = code;
        this.visibleInInbox = visibleInInbox;
    }

    public String getCode() {
        return code;
    }

    public boolean isVisibleInInbox() {
        return visibleInInbox;
    }

    public static InboxStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("InboxStatus code is null");
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown InboxStatus code: " + code));
    }
}
