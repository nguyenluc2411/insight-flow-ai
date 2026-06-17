package com.insightflow.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

/** A tenant row in the super-admin tenant list. */
public record AdminTenantListItem(
        UUID id,
        String name,
        String slug,
        String plan,
        String status,
        long userCount,
        Instant trialEndsAt,
        Instant createdAt
) {}
