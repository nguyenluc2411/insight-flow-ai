package com.insightflow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads tenant/user identity headers forwarded by api-gateway and stores them
 * in {@link TenantContextHolder} for the duration of the request.
 *
 * <p>The gateway validates the JWT and sets these headers before routing:
 * <ul>
 *   <li>{@code X-Tenant-Id}      — tenant UUID from JWT claim {@code tenant_id}</li>
 *   <li>{@code X-User-Id}        — user UUID from JWT {@code sub}</li>
 *   <li>{@code X-Correlation-Id} — UUID for distributed tracing</li>
 * </ul>
 *
 * <p>If the headers are absent (public endpoints, health checks), the context
 * is simply not set and {@link TenantContextHolder#get()} returns null.
 * Downstream code calling {@link TenantContextHolder#requireTenantId()} will throw
 * on unauthenticated requests — this is intentional: services must not serve
 * tenant data without a valid tenant context.
 */
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String tenantIdRaw = request.getHeader(SecurityConstants.HEADER_TENANT_ID);
            String userIdRaw   = request.getHeader(SecurityConstants.HEADER_USER_ID);

            if (StringUtils.hasText(tenantIdRaw) && StringUtils.hasText(userIdRaw)) {
                TenantContext ctx = TenantContext.builder()
                        .tenantId(UUID.fromString(tenantIdRaw))
                        .userId(UUID.fromString(userIdRaw))
                        .correlationId(request.getHeader(SecurityConstants.HEADER_CORRELATION_ID))
                        .build();

                TenantContextHolder.set(ctx);
                log.debug("TenantContext loaded: tenantId={} userId={}", tenantIdRaw, userIdRaw);
            }

            chain.doFilter(request, response);

        } finally {
            // Always clear to prevent context leaking to the next request on the same thread
            TenantContextHolder.clear();
        }
    }
}
