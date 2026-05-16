package com.insightflow.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.gateway.util.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    static final String JWT_CLAIMS_ATTR = "jwt.claims";

    private static final int ORDER = 100;
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (isPublicRoute(route)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return rejectWith401(exchange, "missing-token",
                    "Authorization header with Bearer token is required");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            Claims claims = jwtValidator.validate(token);
            exchange.getAttributes().put(JWT_CLAIMS_ATTR, claims);
            return chain.filter(exchange);
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired for path={}", exchange.getRequest().getPath());
            return rejectWith401(exchange, "jwt-expired", "Token has expired");
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.debug("JWT malformed for path={}", exchange.getRequest().getPath());
            return rejectWith401(exchange, "jwt-malformed", "Token is malformed or unsupported");
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return rejectWith401(exchange, "jwt-invalid", "Token validation failed");
        }
    }

    private boolean isPublicRoute(Route route) {
        if (route == null) return false;
        Object publicValue = route.getMetadata().get("public");
        return Boolean.TRUE.equals(publicValue)
                || "true".equalsIgnoreCase(String.valueOf(publicValue));
    }

    private Mono<Void> rejectWith401(ServerWebExchange exchange, String reason, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://insightflow.ai/errors/auth/" + reason);
        body.put("title", "Authentication Failed");
        body.put("status", 401);
        body.put("detail", detail);
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
}
