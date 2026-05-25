package com.insightflow.notification.service.impl;

import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import com.insightflow.notification.service.interfaces.NotificationEventProcessingService;
import com.insightflow.notification.service.interfaces.ProcessedEventService;
import com.insightflow.notification.service.retry.RetryTopicRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProcessingServiceImpl implements NotificationEventProcessingService {

    private final ProcessedEventService processedEventService;
    private final RetryTopicRoutingService retryTopicRoutingService;

    @Override
    @Transactional
    public void process(IncomingNotificationEvent event, String sourceTopic) {
        if (event == null || event.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        boolean recorded = processedEventService.recordIfNotProcessed(event, sourceTopic);
        if (!recorded) {
            log.debug("Duplicate event skipped topic={} eventId={}", sourceTopic, event.eventId());
            return;
        }

        int attempt = retryTopicRoutingService.resolveAttempt(sourceTopic);
        log.info("Notification event accepted topic={} attempt={} eventId={} eventType={} severity={} recipientId={}",
                sourceTopic,
                attempt,
                event.eventId(),
                event.eventType(),
                event.severity(),
                event.recipientId());
    }
}
