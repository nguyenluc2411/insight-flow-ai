package com.insightflow.notification.exception;

import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;

/**
 * A domain event with an already-processed event_id was received. Maps to the
 * shared 409 DUPLICATE_RESOURCE contract so the error response is consistent
 * with the rest of the system.
 */
public class DuplicateEventException extends BusinessException {

    public DuplicateEventException(String eventId) {
        super(ErrorCode.DUPLICATE_RESOURCE, "Duplicate event detected: " + eventId);
    }
}
