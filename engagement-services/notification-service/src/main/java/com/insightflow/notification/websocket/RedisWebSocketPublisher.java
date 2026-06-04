package com.insightflow.notification.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.dto.websocket.WebSocketNotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(WebSocketNotificationPayload payload) {
        if (payload == null) return;
        String channel = "notification:ws:all"; // simple pub/sub channel
        try {
            String message = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, message);
            log.info("Published websocket payload recipientId={} channel={}", payload.getRecipientId(), channel);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize websocket payload: {}", ex.getMessage());
        }
    }
}

