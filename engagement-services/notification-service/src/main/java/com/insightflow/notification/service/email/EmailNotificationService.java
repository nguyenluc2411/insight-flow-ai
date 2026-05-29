package com.insightflow.notification.service.email;

import com.insightflow.notification.entity.Notification;

public interface EmailNotificationService {

    void sendEmail(Notification notification);
}
