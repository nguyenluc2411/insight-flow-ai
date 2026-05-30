package com.insightflow.notification.service.interfaces;

import com.insightflow.notification.dto.request.NotificationPreferenceRequest;
import com.insightflow.notification.dto.response.UserNotificationPreferenceResponse;

import java.util.List;
import java.util.UUID;

/**
 * Per-user notification channel preferences (which type + channel is enabled,
 * minimum severity, mute window).
 */
public interface NotificationPreferenceService {

    List<UserNotificationPreferenceResponse> getPreferences(UUID userId);

    UserNotificationPreferenceResponse upsert(UUID userId, NotificationPreferenceRequest request);
}
