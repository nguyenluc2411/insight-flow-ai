package com.insightflow.notification.websocket.gateway;

public final class WebSocketDestinations {

    public static final String NOTIFICATIONS = "/queue/notifications";
    public static final String UNREAD_COUNT = "/queue/unread-count";
    public static final String PRESENCE = "/topic/presence";
    public static final String BROADCAST = "/topic/broadcast";

    private WebSocketDestinations() {
    }
}

