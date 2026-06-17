package com.insightflow.auth.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A user row shown inside a tenant's admin detail view. */
public record AdminUserItem(
        UUID id,
        String email,
        String fullName,
        String status,
        List<String> roles,
        Instant lastLoginAt,
        Instant createdAt
) {}
