package com.insightflow.common.web.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String detail) {
        super(ErrorCode.RESOURCE_NOT_FOUND, detail);
    }
}
