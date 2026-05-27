package com.insightflow.notification.service.redis;

import java.util.UUID;

public interface UnreadCountCacheService {

    long getUnreadCount(UUID userId);

    long incrementUnread(UUID userId);

    long decrementUnread(UUID userId);

    void setUnreadCount(UUID userId, long count);

    void evict(UUID userId);
}
