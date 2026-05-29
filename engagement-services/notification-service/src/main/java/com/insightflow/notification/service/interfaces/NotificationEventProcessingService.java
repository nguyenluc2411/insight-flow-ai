package com.insightflow.notification.service.interfaces;

import com.insightflow.notification.event.incoming.IncomingNotificationEvent;

public interface NotificationEventProcessingService {

    void process(IncomingNotificationEvent event, String sourceTopic);
}
