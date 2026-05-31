package com.insightflow.security;

import java.util.List;
import java.util.UUID;

public record UserContext(
        UUID userId,
        UUID tenantId,
        String tenantSlug,
        String plan,
        List<String> roles,
        List<String> permissions
) {}
