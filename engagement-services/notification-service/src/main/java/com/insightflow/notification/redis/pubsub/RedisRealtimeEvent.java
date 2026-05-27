package com.insightflow.notification.redis.pubsub;

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
public class RedisRealtimeEvent {
    private RedisRealtimeEventType type;
    private UUID recipientId;
    private String sessionId;
    private Object payload;
    private Instant timestamp;
}
