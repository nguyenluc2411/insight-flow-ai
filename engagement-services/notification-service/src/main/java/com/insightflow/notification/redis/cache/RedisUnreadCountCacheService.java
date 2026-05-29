package com.insightflow.notification.redis.cache;

import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.redis.keys.RedisKeys;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.redis.UnreadCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisUnreadCountCacheService implements UnreadCountCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;

    @Value("${notification.redis.unread.ttl-seconds:300}")
    private long ttlSeconds;

    @Override
    public long getUnreadCount(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String key = RedisKeys.unreadKey(userId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        long count = notificationRepository.countByRecipientIdAndInboxStatus(userId, InboxStatus.UNREAD);
        redisTemplate.opsForValue().set(key, count, Duration.ofSeconds(ttlSeconds));
        return count;
    }

    @Override
    public long incrementUnread(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String key = RedisKeys.unreadKey(userId);
        Long value = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        return value != null ? value : 0;
    }

    @Override
    public long decrementUnread(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String key = RedisKeys.unreadKey(userId);
        Long value = redisTemplate.opsForValue().decrement(key);
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        return value != null ? Math.max(value, 0) : 0;
    }

    @Override
    public void setUnreadCount(UUID userId, long count) {
        if (userId == null) {
            return;
        }
        String key = RedisKeys.unreadKey(userId);
        redisTemplate.opsForValue().set(key, count, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void evict(UUID userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(RedisKeys.unreadKey(userId));
    }
}
