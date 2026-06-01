package com.insightflow.notification.service.interfaces;

import com.insightflow.common.events.notification.IncomingNotificationEvent;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventService {

    boolean recordIfNotProcessed(IncomingNotificationEvent event, String sourceTopic);

    boolean recordIfNotProcessed(
            UUID eventId,
            String eventType,
            UUID correlationId,
            String sourceService,
            Instant processedAt,
            Object payload,
            String sourceTopic);
}

