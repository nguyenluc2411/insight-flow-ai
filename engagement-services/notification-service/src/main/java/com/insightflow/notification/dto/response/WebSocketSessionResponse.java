package com.insightflow.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class WebSocketSessionResponse {
    private UUID id;
    private String sessionId;
    private UUID userId;
    private String nodeId;
    private String clientId;
    private boolean active;
    private Instant connectedAt;
    private Instant lastHeartbeatAt;
    private Instant updatedAt;
}
