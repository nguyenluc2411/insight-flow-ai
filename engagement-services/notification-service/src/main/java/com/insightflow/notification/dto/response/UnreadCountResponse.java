package com.insightflow.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UnreadCountResponse {
    private UUID userId;
    private long unreadCount;
    private Instant timestamp;
}
