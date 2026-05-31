package com.insightflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String userIdHeader   = request.getHeader(InternalHeaders.X_USER_ID);
        String tenantIdHeader = request.getHeader(InternalHeaders.X_TENANT_ID);

        // Public endpoint — neither header present, skip context population
        if (userIdHeader == null && tenantIdHeader == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID userId   = userIdHeader   != null ? UUID.fromString(userIdHeader)   : null;
            UUID tenantId = tenantIdHeader != null ? UUID.fromString(tenantIdHeader) : null;

            String tenantSlug   = request.getHeader(InternalHeaders.X_TENANT_SLUG);
            String plan         = request.getHeader(InternalHeaders.X_TENANT_PLAN);
            String rolesRaw     = request.getHeader(InternalHeaders.X_USER_ROLES);
            String permsRaw     = request.getHeader(InternalHeaders.X_USER_PERMISSIONS);

            List<String> roles = (rolesRaw != null && !rolesRaw.isBlank())
                    ? List.of(rolesRaw.split(",")) : List.of();
            List<String> permissions = (permsRaw != null && !permsRaw.isBlank())
                    ? List.of(permsRaw.split(",")) : List.of();

            UserContextHolder.set(new UserContext(userId, tenantId, tenantSlug, plan, roles, permissions));
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
