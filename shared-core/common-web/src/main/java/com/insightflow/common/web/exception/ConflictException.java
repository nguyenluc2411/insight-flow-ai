package com.insightflow.common.web.exception;

public class ConflictException extends BusinessException {

    public ConflictException(String detail) {
        super(ErrorCode.DUPLICATE_RESOURCE, detail);
    }
}
