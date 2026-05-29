package com.insightflow.notification.service.websocket;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.websocket.payload.NotificationBroadcastPayload;

import java.util.UUID;

public interface RealtimeNotificationService {

    void pushNotification(Notification notification);

    void pushUnreadCount(UUID recipientId);

    void pushPresence(UUID userId, String sessionId, boolean online);

    void broadcast(NotificationBroadcastPayload payload);
}
