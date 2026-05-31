package com.insightflow.notification.redis.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.websocket.gateway.WebSocketDestinations;
import com.insightflow.notification.websocket.gateway.WebSocketGateway;
import com.insightflow.notification.websocket.payload.NotificationBroadcastPayload;
import com.insightflow.notification.websocket.payload.PresencePayload;
import com.insightflow.notification.websocket.payload.RealtimeNotificationPayload;
import com.insightflow.notification.websocket.payload.UnreadCountPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisNotificationSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final WebSocketGateway webSocketGateway;
    private final GenericJackson2JsonRedisSerializer serializer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Object decoded = serializer.deserialize(message.getBody());
            RedisRealtimeEvent event = objectMapper.convertValue(decoded, RedisRealtimeEvent.class);
            if (event == null || event.getType() == null) {
                return;
            }

            switch (event.getType()) {
                case NOTIFICATION -> {
                    RealtimeNotificationPayload payload = objectMapper.convertValue(event.getPayload(), RealtimeNotificationPayload.class);
                    if (payload != null && payload.getRecipientId() != null) {
                        webSocketGateway.sendToUser(payload.getRecipientId(), WebSocketDestinations.NOTIFICATIONS, payload);
                    }
                }
                case UNREAD_COUNT -> {
                    UnreadCountPayload payload = objectMapper.convertValue(event.getPayload(), UnreadCountPayload.class);
                    if (payload != null && payload.getRecipientId() != null) {
                        webSocketGateway.sendToUser(payload.getRecipientId(), WebSocketDestinations.UNREAD_COUNT, payload);
                    }
                }
                case PRESENCE -> {
                    PresencePayload payload = objectMapper.convertValue(event.getPayload(), PresencePayload.class);
                    if (payload != null) {
                        webSocketGateway.sendToTopic(WebSocketDestinations.PRESENCE, payload);
                    }
                }
                case BROADCAST -> {
                    NotificationBroadcastPayload payload = objectMapper.convertValue(event.getPayload(), NotificationBroadcastPayload.class);
                    if (payload != null) {
                        webSocketGateway.sendToTopic(WebSocketDestinations.BROADCAST, payload);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to handle redis realtime event error={}", ex.getMessage());
        }
    }
}

