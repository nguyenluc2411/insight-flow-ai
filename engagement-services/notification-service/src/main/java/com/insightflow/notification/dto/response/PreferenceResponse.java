package com.insightflow.notification.dto.response;

import com.insightflow.notification.entity.NotificationPreference;

import java.util.Map;
import java.util.UUID;

public record PreferenceResponse(
        UUID id,
        UUID tenantId,
        String eventType,
        String channel,
        boolean enabled,
        Map<String, Object> threshold
) {
    public static PreferenceResponse from(NotificationPreference p) {
        return new PreferenceResponse(
                p.getId(), p.getTenantId(),
                p.getEventType(), p.getChannel(),
                p.isEnabled(), p.getThreshold()
        );
    }
}
