package com.insightflow.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://insightflow.ai/errors/";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_TYPE + "not-found"));
        pd.setTitle("Resource Not Found");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Required header '" + ex.getHeaderName() + "' is not present");
        pd.setType(URI.create(BASE_TYPE + "missing-header"));
        pd.setTitle("Missing Required Header");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName());
        pd.setType(URI.create(BASE_TYPE + "invalid-parameter"));
        pd.setTitle("Invalid Parameter");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setType(URI.create(BASE_TYPE + "validation"));
        pd.setTitle("Validation Failed");
        pd.setProperty("fields", fields);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create(BASE_TYPE + "internal"));
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred. Please try again.");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
