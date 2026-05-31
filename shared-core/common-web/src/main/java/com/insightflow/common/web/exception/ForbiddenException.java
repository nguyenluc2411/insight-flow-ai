package com.insightflow.common.web.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(String detail) {
        super(ErrorCode.FORBIDDEN, detail);
    }
}
