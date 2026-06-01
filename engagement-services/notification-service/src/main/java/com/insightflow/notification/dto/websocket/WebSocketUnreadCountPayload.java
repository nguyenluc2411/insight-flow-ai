package com.insightflow.notification.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class WebSocketUnreadCountPayload {
    private UUID recipientId;
    private long unreadCount;
    private Instant timestamp;
}

