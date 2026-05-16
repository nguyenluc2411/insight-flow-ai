package com.insightflow.gateway.filter;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class TenantContextFilter implements GlobalFilter, Ordered {

    private static final int ORDER = 200;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Claims claims = exchange.getAttribute(JwtAuthenticationFilter.JWT_CLAIMS_ATTR);
        if (claims == null) {
            // Public route — JWT filter skipped, no claims to propagate
            return chain.filter(exchange);
        }

        String tenantId = String.valueOf(claims.get("tenant_id"));
        String userId = claims.getSubject();
        String tenantSlug = claims.get("tenant_slug", String.class);
        if (tenantSlug == null) tenantSlug = "";

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        String rolesHeader = (roles != null && !roles.isEmpty()) ? String.join(",", roles) : "";

        final String finalTenantSlug = tenantSlug;
        final String finalRolesHeader = rolesHeader;

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(headers -> {
                    // Strip any client-supplied values first to prevent spoofing
                    headers.remove("X-Tenant-Id");
                    headers.remove("X-User-Id");
                    headers.remove("X-Tenant-Slug");
                    headers.remove("X-User-Roles");
                    headers.add("X-Tenant-Id", tenantId);
                    headers.add("X-User-Id", userId);
                    headers.add("X-Tenant-Slug", finalTenantSlug);
                    headers.add("X-User-Roles", finalRolesHeader);
                }))
                .build();

        log.debug("Propagated tenant context tenantId={} userId={}", tenantId, userId);
        return chain.filter(mutated);
    }
}
