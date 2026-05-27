package com.insightflow.notification.service.impl;

import com.insightflow.notification.dto.kafka.IncomingNotificationEventDto;
import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import com.insightflow.notification.service.interfaces.NotificationEventProcessingService;
import com.insightflow.notification.service.orchestrator.NotificationOrchestrator;
import com.insightflow.notification.service.retry.RetryTopicRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProcessingServiceImpl implements NotificationEventProcessingService {

    private final RetryTopicRoutingService retryTopicRoutingService;
    private final NotificationOrchestrator notificationOrchestrator;

    @Override
    @Transactional
    public void process(IncomingNotificationEvent event, String sourceTopic) {
        if (event == null || event.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        int attempt = retryTopicRoutingService.resolveAttempt(sourceTopic);
        log.info("Notification event accepted topic={} attempt={} eventId={} eventType={} severity={} recipientId={}",
                sourceTopic,
                attempt,
                event.eventId(),
                event.eventType(),
                event.severity(),
                event.recipientId());

        IncomingNotificationEventDto dto = new IncomingNotificationEventDto();
        dto.setEventId(event.eventId());
        dto.setEventType(event.eventType());
        dto.setTimestamp(event.timestamp() != null ? event.timestamp() : Instant.now());
        dto.setRecipientId(event.recipientId());
        dto.setSeverity(event.severity());
        dto.setTitle(event.title());
        dto.setMessage(event.message());
        dto.setProductId(event.productId());
        dto.setWarehouseId(event.warehouseId());
        dto.setCorrelationId(event.correlationId());
        dto.setSourceService(event.sourceService());
        dto.setPayload(event.payload());

        notificationOrchestrator.orchestrate(dto);
    }
}
