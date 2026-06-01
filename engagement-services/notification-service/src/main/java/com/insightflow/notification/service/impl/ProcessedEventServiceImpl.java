package com.insightflow.notification.service.impl;

import com.insightflow.notification.entity.ProcessedEvent;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.notification.repository.ProcessedEventRepository;
import com.insightflow.notification.service.interfaces.ProcessedEventService;
import com.insightflow.notification.util.PayloadHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventServiceImpl implements ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;
    private final PayloadHashService payloadHashService;

    @Override
    @Transactional
    public boolean recordIfNotProcessed(IncomingNotificationEvent event, String sourceTopic) {
        return recordIfNotProcessed(
                event.eventId(),
                event.eventType(),
                event.correlationId(),
                event.sourceService(),
                event.timestamp(),
                event,
                sourceTopic);
    }

    @Override
    @Transactional
    public boolean recordIfNotProcessed(
            UUID eventId,
            String eventType,
            UUID correlationId,
            String sourceService,
            Instant processedAt,
            Object payload,
            String sourceTopic) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setEventType(eventType);
        processedEvent.setCorrelationId(correlationId);
        processedEvent.setSourceService(sourceService);
        processedEvent.setPayloadHash(payloadHashService.hash(payload));
        processedEvent.setProcessedAt(processedAt != null ? processedAt : processedEvent.getProcessedAt());

        try {
            processedEventRepository.save(processedEvent);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate event ignored topic={} eventId={}", sourceTopic, eventId);
            return false;
        }
    }
}

