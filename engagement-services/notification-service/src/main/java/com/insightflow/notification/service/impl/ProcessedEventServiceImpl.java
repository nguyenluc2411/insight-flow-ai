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

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventServiceImpl implements ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;
    private final PayloadHashService payloadHashService;

    @Override
    @Transactional
    public boolean recordIfNotProcessed(IncomingNotificationEvent event, String sourceTopic) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.eventId());
        processedEvent.setEventType(event.eventType());
        processedEvent.setCorrelationId(event.correlationId());
        processedEvent.setSourceService(event.sourceService());
        processedEvent.setPayloadHash(payloadHashService.hash(event));
        processedEvent.setProcessedAt(event.timestamp() != null ? event.timestamp() : processedEvent.getProcessedAt());

        try {
            processedEventRepository.save(processedEvent);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate event ignored topic={} eventId={}", sourceTopic, event.eventId());
            return false;
        }
    }
}

