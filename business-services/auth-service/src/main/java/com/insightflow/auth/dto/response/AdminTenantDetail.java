package com.insightflow.auth.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Full tenant view for the super-admin, including its users. */
public record AdminTenantDetail(
        UUID id,
        String name,
        String slug,
        String plan,
        String status,
        Instant trialEndsAt,
        Instant createdAt,
        Map<String, Object> settings,
        List<AdminUserItem> users
) {}
