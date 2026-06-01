package com.insightflow.notification.redis.keys;

import java.util.UUID;

public final class RedisKeys {

    public static final String REALTIME_CHANNEL = "notification:realtime";

    private static final String UNREAD_PREFIX = "notification:unread:";
    private static final String ONLINE_PREFIX = "notification:online:";
    private static final String RATE_PREFIX = "notification:rate:";
    private static final String SESSION_PREFIX = "notification:ws:session:";

    private RedisKeys() {
    }

    public static String unreadKey(UUID userId) {
        return UNREAD_PREFIX + userId;
    }

    public static String onlineKey(UUID userId) {
        return ONLINE_PREFIX + userId;
    }

    public static String rateKey(UUID userId) {
        return RATE_PREFIX + userId;
    }

    public static String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }
}

