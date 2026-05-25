package com.insightflow.notification.exception;

import org.springframework.http.HttpStatus;

public class NotificationDeliveryException extends BusinessException {

    public NotificationDeliveryException(String message) {
        super(message, "NOTIFICATION_DELIVERY_FAILED", HttpStatus.BAD_GATEWAY);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, "NOTIFICATION_DELIVERY_FAILED", HttpStatus.BAD_GATEWAY, cause);
    }
}
