package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.Recommendation;
import com.insightflow.recommendation.enums.RecommendationStatus;
import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID>, JpaSpecificationExecutor<Recommendation> {

    Page<Recommendation> findByStatus(RecommendationStatus status, Pageable pageable);

    Page<Recommendation> findByRecommendationType(RecommendationType recommendationType, Pageable pageable);

    Optional<Recommendation> findByEventId(UUID eventId);

    long countByRecommendationType(RecommendationType recommendationType);

    long countByRiskLevelIn(Collection<RiskLevel> riskLevels);

    @Query("select avg(r.confidenceScore) from Recommendation r")
    BigDecimal findAverageConfidenceScore();
}
