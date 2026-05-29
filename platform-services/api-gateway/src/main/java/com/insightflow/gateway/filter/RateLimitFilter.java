package com.insightflow.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.gateway.util.ServiceTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Synchronous per-tenant API quota enforcement.
 *
 * Runs after {@link TenantContextFilter} (order -5) so the resolved {@code X-Tenant-Id}
 * header is available. For every authenticated request it asks billing-service to count
 * the call and report whether the tenant is still within their plan's daily quota.
 *
 * Fail-open: if billing-service is unreachable or the tenant has no subscription, the
 * request is allowed — a billing outage must not take down the whole platform.
 */
@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    // After TenantContextFilter (-5), before route GatewayFilters (1).
    private static final int ORDER = 0;
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String CHECK_URI = "http://billing-service/api/v1/internal/tenants/{tenantId}/usage/check";

    private final WebClient lbWebClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(@Qualifier("lbWebClient") WebClient lbWebClient,
                           ServiceTokenProvider serviceTokenProvider,
                           ObjectMapper objectMapper) {
        this.lbWebClient = lbWebClient;
        this.serviceTokenProvider = serviceTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst(TENANT_HEADER);

        // No tenant context → public/unauthenticated route, nothing to meter.
        if (!StringUtils.hasText(tenantId)) {
            return chain.filter(exchange);
        }

        return lbWebClient.post()
                .uri(CHECK_URI, tenantId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getToken())
                .retrieve()
                .bodyToMono(RateLimit.class)
                .flatMap(limit -> {
                    if (!limit.allowed()) {
                        return rejectWith429(exchange);
                    }
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Remaining", String.valueOf(limit.remaining()));
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    // Fail-open: billing down, no subscription (404), timeout, etc.
                    log.warn("Rate-limit check failed (fail-open) tenant={}: {}", tenantId, e.toString());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> rejectWith429(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(HttpHeaders.RETRY_AFTER, "60");

        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://insightflow.ai/errors/rate-limit-exceeded");
        body.put("title", "Rate Limit Exceeded");
        body.put("status", 429);
        body.put("detail", "Daily API call limit for your plan has been reached. Upgrade your plan or try again tomorrow.");
        body.put("correlationId", correlationId != null ? correlationId : "unknown");
        body.put("timestamp", Instant.now().toString());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    /** Subset of billing's RateLimitResponse needed by the gateway. */
    private record RateLimit(boolean allowed, int remaining, int limit, int rateLimitPerMinute) {
    }
}
