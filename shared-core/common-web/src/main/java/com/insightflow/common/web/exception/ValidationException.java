package com.insightflow.common.web.exception;

import com.insightflow.common.web.response.FieldError;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends BusinessException {
    private final List<FieldError> fieldErrors;

    public ValidationException(String detail, List<FieldError> fieldErrors) {
        super(ErrorCode.VALIDATION_FAILED, detail);
        this.fieldErrors = fieldErrors != null ? fieldErrors : List.of();
    }
}
