package com.insightflow.common.web.exception;

public class TenantAccessException extends BusinessException {

    public TenantAccessException() {
        super(ErrorCode.TENANT_ACCESS_DENIED, "Access to this resource is not allowed for your tenant");
    }

    public TenantAccessException(String detail) {
        super(ErrorCode.TENANT_ACCESS_DENIED, detail);
    }
}
