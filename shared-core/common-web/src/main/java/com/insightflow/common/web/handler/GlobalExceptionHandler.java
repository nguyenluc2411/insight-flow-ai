package com.insightflow.common.web.handler;

import com.insightflow.common.web.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://insightflow.ai/errors/";
    // Header forwarded by CorrelationIdFilter in api-gateway
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, WebRequest request) {
        log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                ex.getErrorCode().getHttpStatus(), ex.getMessage());
        problem.setType(toTypeUri(ex.getErrorCode().getCode()));
        problem.setTitle(ex.getErrorCode().getCode());
        problem.setProperty("errorCode", ex.getErrorCode().getCode());
        enrichWithCorrelationId(problem, request);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value",
                        (a, b) -> a));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body validation failed");
        problem.setType(toTypeUri("VALIDATION_FAILED"));
        problem.setTitle("Validation Failed");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", fieldErrors);
        enrichWithCorrelationId(problem, request);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, WebRequest request) {
        log.debug("Malformed request body: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body is malformed or missing");
        problem.setType(toTypeUri("INVALID_REQUEST"));
        problem.setTitle("Invalid Request");
        problem.setProperty("errorCode", "INVALID_REQUEST");
        enrichWithCorrelationId(problem, request);
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing");
        problem.setType(toTypeUri("INVALID_REQUEST"));
        problem.setTitle("Invalid Request");
        problem.setProperty("errorCode", "INVALID_REQUEST");
        enrichWithCorrelationId(problem, request);
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' has invalid type");
        problem.setType(toTypeUri("INVALID_REQUEST"));
        problem.setTitle("Invalid Request");
        problem.setProperty("errorCode", "INVALID_REQUEST");
        enrichWithCorrelationId(problem, request);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(toTypeUri("INTERNAL_ERROR"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        enrichWithCorrelationId(problem, request);
        return problem;
    }

    private static URI toTypeUri(String errorCode) {
        return URI.create(ERROR_TYPE_BASE + errorCode.toLowerCase().replace('_', '-'));
    }

    private static void enrichWithCorrelationId(ProblemDetail problem, WebRequest request) {
        problem.setProperty("timestamp", Instant.now().toString());
        String correlationId = request.getHeader(HEADER_CORRELATION_ID);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
    }
}
