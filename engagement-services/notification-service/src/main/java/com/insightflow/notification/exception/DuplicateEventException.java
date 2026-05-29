package com.insightflow.notification.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEventException extends BusinessException {

    public DuplicateEventException(String eventId) {
        super("Duplicate event detected: " + eventId, "DUPLICATE_EVENT", HttpStatus.CONFLICT);
    }
}
