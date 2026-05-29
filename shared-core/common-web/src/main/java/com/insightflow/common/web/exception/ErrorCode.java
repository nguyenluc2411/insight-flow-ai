package com.insightflow.common.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 400
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request", HttpStatus.BAD_REQUEST),

    // 401
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("TOKEN_INVALID", "Token is invalid", HttpStatus.UNAUTHORIZED),

    // 403
    FORBIDDEN("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN),
    TENANT_MISMATCH("TENANT_MISMATCH", "Resource does not belong to your tenant", HttpStatus.FORBIDDEN),

    // 404
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),

    // 409
    CONFLICT("CONFLICT", "Conflict with current state", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Resource already exists", HttpStatus.CONFLICT),

    // 500
    INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    DOWNSTREAM_ERROR("DOWNSTREAM_ERROR", "Downstream service error", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
