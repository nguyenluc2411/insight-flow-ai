package com.insightflow.common.web.response;

import com.insightflow.common.web.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ApiError {
    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final String correlationId;
    private final Instant timestamp;
    private final List<FieldError> errors;

    public static ApiError of(ErrorCode code, String detail, HttpServletRequest request) {
        return of(code, detail, request, null);
    }

    public static ApiError of(ErrorCode code, String detail, HttpServletRequest request, List<FieldError> errors) {
        return ApiError.builder()
                .type("https://insightflow.ai/errors/" + code.getCode().toLowerCase().replace('_', '-'))
                .title(code.getDefaultMessage())
                .status(code.getHttpStatus().value())
                .detail(detail)
                .instance(request.getRequestURI())
                .correlationId(request.getHeader("X-Correlation-Id"))
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
}
