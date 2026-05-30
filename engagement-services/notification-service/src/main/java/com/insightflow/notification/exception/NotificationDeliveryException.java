package com.insightflow.notification.exception;

/**
 * Raised when a delivery channel (email/websocket) fails transiently.
 * Treated as RETRYABLE by the retry/DLQ router, so it is intentionally NOT a
 * common-web BusinessException (those are permanent / non-retryable). It is an
 * internal async signal and is never serialized to an HTTP response.
 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
