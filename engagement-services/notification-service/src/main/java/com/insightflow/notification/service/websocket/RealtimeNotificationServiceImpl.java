package com.insightflow.notification.service.websocket;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.mapper.NotificationWebSocketMapper;
import com.insightflow.notification.redis.pubsub.RedisNotificationPublisher;
import com.insightflow.notification.redis.pubsub.RedisRealtimeEvent;
import com.insightflow.notification.redis.pubsub.RedisRealtimeEventType;
import com.insightflow.notification.service.redis.UnreadCountCacheService;
import com.insightflow.notification.websocket.payload.NotificationBroadcastPayload;
import com.insightflow.notification.websocket.payload.PresencePayload;
import com.insightflow.notification.websocket.payload.RealtimeNotificationPayload;
import com.insightflow.notification.websocket.payload.UnreadCountPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationServiceImpl implements RealtimeNotificationService {

    private final RedisNotificationPublisher redisNotificationPublisher;
    private final NotificationWebSocketMapper webSocketMapper;
    private final UnreadCountCacheService unreadCountCacheService;

    @Override
    public void pushNotification(Notification notification) {
        if (notification == null || notification.getRecipientId() == null) {
            return;
        }
        RealtimeNotificationPayload payload = webSocketMapper.toPayload(notification);
        RedisRealtimeEvent event = RedisRealtimeEvent.builder()
                .type(RedisRealtimeEventType.NOTIFICATION)
                .recipientId(notification.getRecipientId())
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        redisNotificationPublisher.publish(event);

        long unreadCount = unreadCountCacheService.incrementUnread(notification.getRecipientId());
        pushUnreadCount(notification.getRecipientId(), unreadCount);
    }

    @Override
    public void pushUnreadCount(UUID recipientId) {
        if (recipientId == null) {
            return;
        }
        long count = unreadCountCacheService.getUnreadCount(recipientId);
        pushUnreadCount(recipientId, count);
    }

    @Override
    public void pushPresence(UUID userId, String sessionId, boolean online) {
        if (userId == null) {
            return;
        }
        PresencePayload payload = PresencePayload.builder()
                .userId(userId)
                .sessionId(sessionId)
                .online(online)
                .timestamp(Instant.now())
                .build();

        RedisRealtimeEvent event = RedisRealtimeEvent.builder()
                .type(RedisRealtimeEventType.PRESENCE)
                .recipientId(userId)
                .sessionId(sessionId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        redisNotificationPublisher.publish(event);
    }

    @Override
    public void broadcast(NotificationBroadcastPayload payload) {
        if (payload == null) {
            return;
        }
        RedisRealtimeEvent event = RedisRealtimeEvent.builder()
                .type(RedisRealtimeEventType.BROADCAST)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        redisNotificationPublisher.publish(event);
    }

    private void pushUnreadCount(UUID recipientId, long count) {
        UnreadCountPayload payload = UnreadCountPayload.builder()
                .recipientId(recipientId)
                .unreadCount(count)
                .timestamp(Instant.now())
                .build();

        RedisRealtimeEvent event = RedisRealtimeEvent.builder()
                .type(RedisRealtimeEventType.UNREAD_COUNT)
                .recipientId(recipientId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        redisNotificationPublisher.publish(event);
    }
}
