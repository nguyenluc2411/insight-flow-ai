package com.insightflow.notification.service.redis;

import java.util.Set;
import java.util.UUID;

public interface PresenceService {

    void markOnline(UUID userId, String sessionId);

    void markOffline(UUID userId, String sessionId);

    boolean isOnline(UUID userId);

    Set<String> getActiveSessions(UUID userId);
}
