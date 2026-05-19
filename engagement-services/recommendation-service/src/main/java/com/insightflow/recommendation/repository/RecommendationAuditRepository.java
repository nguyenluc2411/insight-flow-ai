package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.RecommendationAudit;
import com.insightflow.recommendation.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecommendationAuditRepository extends JpaRepository<RecommendationAudit, UUID> {

    Page<RecommendationAudit> findByProcessingStatus(ProcessingStatus processingStatus, Pageable pageable);

    Page<RecommendationAudit> findByEventId(UUID eventId, Pageable pageable);
}
