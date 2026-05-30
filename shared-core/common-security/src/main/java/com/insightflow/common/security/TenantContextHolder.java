package com.insightflow.common.security;

import java.util.UUID;

/**
 * ThreadLocal holder for {@link TenantContext}.
 * Managed by {@link TenantContextFilter} — do NOT call set/clear manually from business code.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static TenantContext get() {
        return CONTEXT.get();
    }

    /**
     * Returns the tenantId for the current request.
     * Throws if the context is not set — this means the endpoint is missing tenant resolution.
     */
    public static UUID requireTenantId() {
        TenantContext ctx = CONTEXT.get();
        if (ctx == null || ctx.getTenantId() == null) {
            throw new IllegalStateException("TenantContext is not set — endpoint may be missing tenant resolution");
        }
        return ctx.getTenantId();
    }

    /**
     * Returns the userId for the current request.
     * Throws if the context is not set.
     */
    public static UUID requireUserId() {
        TenantContext ctx = CONTEXT.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw new IllegalStateException("TenantContext is not set — endpoint may be missing tenant resolution");
        }
        return ctx.getUserId();
    }

    /** Removes the context from the current thread. Always called in the filter's finally block. */
    public static void clear() {
        CONTEXT.remove();
    }

    private TenantContextHolder() {}
}
