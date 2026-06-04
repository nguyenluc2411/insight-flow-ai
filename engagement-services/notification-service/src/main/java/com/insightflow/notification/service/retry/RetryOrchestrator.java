package com.insightflow.notification.service.retry;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDeliveryHistory;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.common.events.notification.NotificationRetryEvent;

public interface RetryOrchestrator {

    void handleDeliveryFailure(
            Notification notification,
            NotificationChannel channel,
            String failureReason,
            Throwable exception,
            NotificationDeliveryHistory deliveryHistory);

    void handleRetryEvent(NotificationRetryEvent event);

    void markRetrySuccess(Notification notification, NotificationChannel channel);

    void replayFromDlq(Notification notification, NotificationChannel channel, String failureReason);
}

