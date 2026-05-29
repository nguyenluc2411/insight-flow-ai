package com.insightflow.notification.redis.cache;

import com.insightflow.notification.redis.keys.RedisKeys;
import com.insightflow.notification.service.redis.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPresenceService implements PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${notification.redis.presence.ttl-seconds:300}")
    private long ttlSeconds;

    @Override
    public void markOnline(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        String onlineKey = RedisKeys.onlineKey(userId);
        redisTemplate.opsForSet().add(onlineKey, sessionId);
        redisTemplate.expire(onlineKey, Duration.ofSeconds(ttlSeconds));

        String sessionKey = RedisKeys.sessionKey(sessionId);
        redisTemplate.opsForValue().set(sessionKey, userId.toString(), Duration.ofSeconds(ttlSeconds));

        log.info("User online userId={} sessionId={}", userId, sessionId);
    }

    @Override
    public void markOffline(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        String onlineKey = RedisKeys.onlineKey(userId);
        redisTemplate.opsForSet().remove(onlineKey, sessionId);
        redisTemplate.delete(RedisKeys.sessionKey(sessionId));

        Long size = redisTemplate.opsForSet().size(onlineKey);
        if (size == null || size == 0) {
            redisTemplate.delete(onlineKey);
        }

        log.info("User offline userId={} sessionId={}", userId, sessionId);
    }

    @Override
    public boolean isOnline(UUID userId) {
        if (userId == null) {
            return false;
        }
        String onlineKey = RedisKeys.onlineKey(userId);
        Long size = redisTemplate.opsForSet().size(onlineKey);
        return size != null && size > 0;
    }

    @Override
    public Set<String> getActiveSessions(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String onlineKey = RedisKeys.onlineKey(userId);
        Set<Object> members = redisTemplate.opsForSet().members(onlineKey);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
    }
}
