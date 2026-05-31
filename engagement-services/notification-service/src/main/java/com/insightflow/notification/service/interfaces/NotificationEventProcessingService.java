package com.insightflow.notification.service.interfaces;

import com.insightflow.common.events.notification.IncomingNotificationEvent;

public interface NotificationEventProcessingService {

    void process(IncomingNotificationEvent event, String sourceTopic);
}

