package com.insightflow.gateway.ratelimit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limiter backed by Redis Lua script for atomic increment + expire.
 * Intended for auth endpoints only (L1 IP-based, 5 req / 15 min).
 * Token-bucket limiters (RedisRateLimiter) are used for all other routes.
 */
@Slf4j
public class FixedWindowRateLimiter implements RateLimiter<FixedWindowRateLimiter.Config> {

    private static final String LUA_SCRIPT =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "local ttl = redis.call('TTL', KEYS[1])\n" +
            "return {current, math.max(0, tonumber(ARGV[2]) - current), ttl}";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> script;
    private final int maxRequests;
    private final int windowSizeSeconds;
    private final Map<String, Config> configMap = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(ReactiveStringRedisTemplate redisTemplate,
                                   int maxRequests, int windowSizeSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSizeSeconds = windowSizeSeconds;
        this.script = RedisScript.of(LUA_SCRIPT, Long.class);
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public Map<String, Config> getConfig() {
        return configMap;
    }

    @Override
    public Config newConfig() {
        Config config = new Config();
        config.setMaxRequests(maxRequests);
        config.setWindowSizeSeconds(windowSizeSeconds);
        return config;
    }

    @Override
    public reactor.core.publisher.Mono<Response> isAllowed(String routeId, String id) {
        String key = "rl:fixed:" + routeId + ":" + id;
        String windowStr = String.valueOf(windowSizeSeconds);
        String maxStr = String.valueOf(maxRequests);

        return redisTemplate.execute(script, List.of(key), windowStr, maxStr)
                .collectList()
                .map(results -> {
                    if (results.size() < 2) {
                        log.warn("Unexpected Lua result size={} for key={}", results.size(), key);
                        return new Response(true, Map.of());
                    }
                    long count = results.get(0);
                    long remaining = results.get(1);
                    boolean allowed = count <= maxRequests;
                    return new Response(allowed, Map.of(
                            "X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)),
                            "X-RateLimit-Limit", String.valueOf(maxRequests)
                    ));
                })
                .onErrorResume(e -> {
                    log.error("FixedWindowRateLimiter Redis error for key={}: {}", key, e.getMessage());
                    return reactor.core.publisher.Mono.just(new Response(true, Map.of())); // fail open
                });
    }

    @Data
    public static class Config {
        private int maxRequests;
        private int windowSizeSeconds;
    }
}
