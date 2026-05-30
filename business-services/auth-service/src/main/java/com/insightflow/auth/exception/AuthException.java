package com.insightflow.auth.exception;

import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;

public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
