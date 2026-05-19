package com.insightflow.bff.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://insightflow.ai/errors/";

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(BASE_TYPE + "missing-header"));
        pd.setTitle("Missing Required Header");
        pd.setDetail("Required header '" + ex.getHeaderName() + "' is not present");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(BASE_TYPE + "invalid-parameter"));
        pd.setTitle("Invalid Parameter");
        pd.setDetail("Parameter '" + ex.getName() + "' has invalid value: " + ex.getValue());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ProblemDetail handleWebClientError(WebClientRequestException ex) {
        log.error("Downstream service connection failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        pd.setType(URI.create(BASE_TYPE + "downstream-unavailable"));
        pd.setTitle("Downstream Service Unavailable");
        pd.setDetail("Unable to connect to a required downstream service. Try again later.");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error in dashboard-bff", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create(BASE_TYPE + "internal-error"));
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred. Please try again.");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
