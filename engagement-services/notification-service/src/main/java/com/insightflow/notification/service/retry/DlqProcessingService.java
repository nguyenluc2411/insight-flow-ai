package com.insightflow.notification.service.retry;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.common.events.notification.NotificationRetryEvent;

public interface DlqProcessingService {

    void recordDlq(
            Notification notification,
            NotificationChannel channel,
            String failureReason,
            FailureType failureType,
            int retryCount);

    void recordDlqMissingNotification(NotificationRetryEvent event, String failureReason);

    void recordDlqIncomingEvent(IncomingNotificationEvent event, String failureReason);
}

