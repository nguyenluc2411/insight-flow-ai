package com.insightflow.billing.config;

import com.insightflow.security.CurrentUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers the shared @CurrentUser argument resolver from common-security so
 * controllers can inject the per-request UserContext (populated by
 * UserContextFilter from the gateway-injected X-Tenant-Id / X-User-Id headers).
 * Without this, @CurrentUser resolves to null.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
