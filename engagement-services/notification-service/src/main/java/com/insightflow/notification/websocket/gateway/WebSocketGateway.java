package com.insightflow.notification.websocket.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketGateway {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(UUID userId, String destination, Object payload) {
        if (userId == null || destination == null || payload == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, payload);
        log.info("WebSocket send userId={} destination={}", userId, destination);
    }

    public void sendToTopic(String destination, Object payload) {
        if (destination == null || payload == null) {
            return;
        }
        messagingTemplate.convertAndSend(destination, payload);
        log.info("WebSocket broadcast destination={}", destination);
    }
}
