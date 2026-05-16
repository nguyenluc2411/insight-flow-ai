package com.insightflow.gateway.exception;

import com.insightflow.gateway.filter.CorrelationIdFilter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                   WebProperties webProperties,
                                   ApplicationContext applicationContext,
                                   ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        super.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        String correlationId = request.headers().firstHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);

        log.error("Unhandled gateway error correlationId={} error={}", correlationId, error.getMessage());

        if (error instanceof ExpiredJwtException) {
            return buildResponse(HttpStatus.UNAUTHORIZED, "jwt-expired",
                    "Token has expired", request.path(), correlationId);
        }
        if (error instanceof JwtException) {
            return buildResponse(HttpStatus.UNAUTHORIZED, "jwt-invalid",
                    "Token validation failed", request.path(), correlationId);
        }
        if (error instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            String detail = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            return buildResponse(status, "request-error", detail, request.path(), correlationId);
        }

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "An unexpected error occurred", request.path(), correlationId);
    }

    private Mono<ServerResponse> buildResponse(HttpStatus status, String errorCode,
                                                String detail, String instance, String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://insightflow.ai/errors/" + errorCode);
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", instance);
        body.put("correlationId", correlationId != null ? correlationId : "unknown");
        body.put("timestamp", Instant.now().toString());

        ServerResponse.BodyBuilder builder = ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON);

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            builder = builder.header("Retry-After", "60");
        }

        return builder.bodyValue(body);
    }
}
