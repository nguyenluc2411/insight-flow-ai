package com.insightflow.common.security;

public final class SecurityConstants {

    // Headers set by api-gateway after JWT validation
    public static final String HEADER_TENANT_ID    = "X-Tenant-Id";
    public static final String HEADER_USER_ID      = "X-User-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    // JWT claim keys — mirrors what auth-service puts in the token
    public static final String CLAIM_TENANT_ID   = "tenant_id";
    public static final String CLAIM_TENANT_SLUG = "tenant_slug";
    public static final String CLAIM_PLAN        = "plan";
    public static final String CLAIM_ROLES       = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";

    private SecurityConstants() {}
}
