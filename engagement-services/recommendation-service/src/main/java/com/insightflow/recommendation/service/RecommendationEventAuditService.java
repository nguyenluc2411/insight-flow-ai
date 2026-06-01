package com.insightflow.recommendation.service;

import com.insightflow.recommendation.entity.RecommendationAudit;
import com.insightflow.recommendation.enums.ProcessingStatus;
import com.insightflow.recommendation.repository.RecommendationAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationEventAuditService {

    private final RecommendationAuditRepository recommendationAuditRepository;

    @Transactional(readOnly = true)
    public boolean isProcessed(UUID eventId) {
        return recommendationAuditRepository.existsByEventId(eventId);
    }

    @Transactional
    public RecommendationAudit recordSuccess(UUID eventId, String eventType, String actionType,
                                             Map<String, Object> payloadSnapshot) {
        RecommendationAudit audit = new RecommendationAudit();
        audit.setEventId(eventId);
        audit.setEventType(eventType);
        audit.setActionType(actionType);
        audit.setProcessingStatus(ProcessingStatus.SUCCESS);
        audit.setPayloadSnapshot(payloadSnapshot);
        audit.setProcessedAt(Instant.now());
        return recommendationAuditRepository.save(audit);
    }

    @Transactional
    public RecommendationAudit recordFailure(UUID eventId, String eventType, String actionType,
                                             String errorMessage, Map<String, Object> payloadSnapshot) {
        RecommendationAudit audit = new RecommendationAudit();
        audit.setEventId(eventId);
        audit.setEventType(eventType);
        audit.setActionType(actionType);
        audit.setProcessingStatus(ProcessingStatus.FAILED);
        audit.setErrorMessage(errorMessage);
        audit.setPayloadSnapshot(payloadSnapshot);
        audit.setProcessedAt(Instant.now());
        return recommendationAuditRepository.save(audit);
    }
}

