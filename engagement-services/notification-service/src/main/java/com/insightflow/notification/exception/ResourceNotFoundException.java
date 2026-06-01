package com.insightflow.notification.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

