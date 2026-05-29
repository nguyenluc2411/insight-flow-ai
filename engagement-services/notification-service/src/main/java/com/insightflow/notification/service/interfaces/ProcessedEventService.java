package com.insightflow.notification.service.interfaces;

import com.insightflow.notification.event.incoming.IncomingNotificationEvent;

public interface ProcessedEventService {

    boolean recordIfNotProcessed(IncomingNotificationEvent event, String sourceTopic);
}
