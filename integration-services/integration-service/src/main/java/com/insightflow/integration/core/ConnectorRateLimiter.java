package com.insightflow.integration.core;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectorRateLimiter {

    private final RateLimiterRegistry rateLimiterRegistry;

    public void acquirePermission(ConnectorType type) {
        String name = type.name().toLowerCase();
        RateLimiter limiter = rateLimiterRegistry.rateLimiter(name);
        limiter.acquirePermission();
        log.debug("Rate limit permission acquired for connector: {}", type);
    }
}
