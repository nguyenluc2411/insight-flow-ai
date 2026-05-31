package com.insightflow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Set;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final int ORDER = -50;
    private static final String REQUEST_START_TIME = "request.startTime";
    private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
            "token", "password", "secret", "api_key", "key", "auth"
    );
    private static final int MAX_USER_AGENT_LENGTH = 100;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(REQUEST_START_TIME, System.currentTimeMillis());

        return Mono.deferContextual(ctx -> {
            String correlationId = ctx.getOrDefault("correlationId", "unknown");
            ServerHttpRequest request = exchange.getRequest();

            String method = request.getMethod().name();
            String path = request.getPath().value();
            String remoteIp = extractRemoteIp(request);
            String userAgent = truncate(request.getHeaders().getFirst("User-Agent"), MAX_USER_AGENT_LENGTH);
            String queryString = sanitizeQueryString(request.getURI().getRawQuery());

            String fullPath = StringUtils.hasText(queryString) ? path + "?" + queryString : path;

            log.info("[{}] >>> method={} path={} from={} ua=\"{}\"",
                    correlationId, method, fullPath, remoteIp, userAgent);

            return chain.filter(exchange)
                    .doFinally(signalType -> {
                        Long start = exchange.getAttribute(REQUEST_START_TIME);
                        long duration = start != null ? System.currentTimeMillis() - start : 0;

                        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                        String code = statusCode != null ? String.valueOf(statusCode.value()) : "unknown";

                        long contentLength = exchange.getResponse().getHeaders().getContentLength();
                        String sizeStr = contentLength >= 0 ? " size=" + contentLength : "";

                        log.info("[{}] <<< status={} duration={}ms{}",
                                correlationId, code, duration, sizeStr);
                    });
        });
    }

    private String extractRemoteIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getHostString() : "unknown";
    }

    private String sanitizeQueryString(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) return "";
        StringBuilder sb = new StringBuilder();
        for (String param : rawQuery.split("&")) {
            if (!sb.isEmpty()) sb.append("&");
            int eq = param.indexOf('=');
            if (eq >= 0) {
                String key = param.substring(0, eq);
                String value = param.substring(eq + 1);
                sb.append(key).append("=");
                sb.append(SENSITIVE_QUERY_PARAMS.contains(key.toLowerCase()) ? "***" : value);
            } else {
                sb.append(param);
            }
        }
        return sb.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
