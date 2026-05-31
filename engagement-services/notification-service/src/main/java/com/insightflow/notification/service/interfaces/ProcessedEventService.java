package com.insightflow.notification.service.interfaces;

import com.insightflow.common.events.notification.IncomingNotificationEvent;

public interface ProcessedEventService {

    boolean recordIfNotProcessed(IncomingNotificationEvent event, String sourceTopic);
}

