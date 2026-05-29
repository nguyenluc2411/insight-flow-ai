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
public class PresencePayload {
    private UUID userId;
    private String sessionId;
    private boolean online;
    private Instant timestamp;
}
