package com.insightflow.notification.websocket.handler;

import com.insightflow.notification.service.redis.PresenceService;
import com.insightflow.notification.service.websocket.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionEventHandler {

    private final PresenceService presenceService;
    private final RealtimeNotificationService realtimeNotificationService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("WebSocket connect missing principal sessionId={}", accessor.getSessionId());
            return;
        }
        UUID userId = parseUserId(principal.getName());
        if (userId == null) {
            log.warn("WebSocket connect invalid userId sessionId={}", accessor.getSessionId());
            return;
        }

        String sessionId = accessor.getSessionId();
        presenceService.markOnline(userId, sessionId);
        realtimeNotificationService.pushPresence(userId, sessionId, true);
        realtimeNotificationService.pushUnreadCount(userId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Principal principal = event.getUser();
        if (principal == null) {
            log.warn("WebSocket disconnect missing principal sessionId={}", sessionId);
            return;
        }
        UUID userId = parseUserId(principal.getName());
        if (userId == null) {
            log.warn("WebSocket disconnect invalid userId sessionId={}", sessionId);
            return;
        }

        presenceService.markOffline(userId, sessionId);
        realtimeNotificationService.pushPresence(userId, sessionId, false);
    }

    private UUID parseUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
