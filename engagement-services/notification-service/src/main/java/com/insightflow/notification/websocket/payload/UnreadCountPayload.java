package com.insightflow.notification.websocket.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountPayload {
    private UUID recipientId;
    private long unreadCount;
    private Instant timestamp;
}

