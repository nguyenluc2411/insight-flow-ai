package com.insightflow.gateway.config;

import com.insightflow.gateway.ratelimit.FixedWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Fixed-window limiter for login/register/refresh routes.
     * Allows 5 requests per 15-minute window per IP.
     */
    @Bean
    public FixedWindowRateLimiter loginRateLimiter() {
        return new FixedWindowRateLimiter(redisTemplate, 5, 900);
    }

    /**
     * Token-bucket limiter for all standard protected routes.
     * Replenish: 5 tokens/sec (~300 req/min sustained), burst: 10.
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Token-bucket limiter for POS webhook routes.
     * Replenish: 10 tokens/sec (~600 req/min sustained), burst: 30.
     */
    @Bean
    public RedisRateLimiter webhookRateLimiter() {
        return new RedisRateLimiter(10, 30, 1);
    }

    /**
     * IP-based key resolver. Respects X-Forwarded-For for reverse-proxy deployments.
     * All rate limiters use this — per gateway-agent.md, user/tenant-based limiting
     * is a downstream service responsibility (L2/L3).
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return Mono.just(forwarded.split(",")[0].trim());
            }
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            return Mono.just(remote != null ? remote.getHostString() : "unknown");
        };
    }
}
