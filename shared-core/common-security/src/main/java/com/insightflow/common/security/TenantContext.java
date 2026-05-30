package com.insightflow.common.security;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of the authenticated principal for the current request.
 * Populated from headers forwarded by api-gateway after JWT validation.
 * Access via {@link TenantContextHolder}.
 */
@Value
@Builder
public class TenantContext {

    UUID tenantId;
    UUID userId;
    String correlationId;

    // Optional fields — present on authenticated requests, null on public endpoints
    String tenantSlug;
    String plan;
    List<String> roles;
    List<String> permissions;
}
