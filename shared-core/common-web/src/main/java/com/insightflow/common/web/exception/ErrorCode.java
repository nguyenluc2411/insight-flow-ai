package com.insightflow.common.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("INVALID_REQUEST", HttpStatus.BAD_REQUEST),

    // 401 Unauthorized
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("TOKEN_INVALID", HttpStatus.UNAUTHORIZED),

    // 403 Forbidden
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN),
    TENANT_ACCESS_DENIED("TENANT_ACCESS_DENIED", HttpStatus.FORBIDDEN),

    // 404 Not Found
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND),

    // 409 Conflict
    CONFLICT("CONFLICT", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", HttpStatus.CONFLICT),

    // 422 Unprocessable Entity
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY),

    // 500 / 502
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final HttpStatus httpStatus;
}
