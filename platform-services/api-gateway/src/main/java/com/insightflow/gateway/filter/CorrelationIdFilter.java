package com.insightflow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Component
@Slf4j
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

        final String correlationId;
        if (StringUtils.hasText(incomingId)) {
            correlationId = incomingId;
            log.debug("Reused correlationId={}", correlationId);
        } else {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated correlationId={}", correlationId);
        }

        // Mutate the exchange so the forwarded request carries the header downstream.
        // We use exchange.mutate().request(...).build() rather than
        // exchange.getRequest().mutate() alone — the latter produces a detached
        // builder that does not propagate back into the exchange.
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // Add the correlation ID to the response so callers can correlate
        // log entries with their request. This must be done before chain.filter()
        // because response headers may be committed immediately on first write.
        mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        // Store the correlation ID in the Reactor Context under the key
        // "correlationId". Hooks.enableAutomaticContextPropagation() (registered
        // in ApiGatewayApplication) bridges this value into MDC on every operator
        // boundary, making %X{correlationId} work in Logback patterns without
        // calling MDC.put() directly (which would be thread-local and unsafe in
        // reactive pipelines).
        return chain.filter(mutatedExchange)
                .contextWrite(Context.of("correlationId", correlationId));
    }
}
