package com.insightflow.common.web.response;

public record FieldError(String field, String message, Object rejectedValue) {

    public static FieldError of(org.springframework.validation.FieldError e) {
        return new FieldError(e.getField(), e.getDefaultMessage(), e.getRejectedValue());
    }
}
