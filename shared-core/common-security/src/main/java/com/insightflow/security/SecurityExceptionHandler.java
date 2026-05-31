package com.insightflow.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/**
 * Handles ResponseStatusException before service-level catch-all handlers.
 * @Order(HIGHEST_PRECEDENCE) ensures this runs before service GlobalExceptionHandlers.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex) {
        int statusCode = ex.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        String detail = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://insightflow.ai/errors/" + status.name().toLowerCase().replace('_', '-')));
        pd.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(pd);
    }
}
