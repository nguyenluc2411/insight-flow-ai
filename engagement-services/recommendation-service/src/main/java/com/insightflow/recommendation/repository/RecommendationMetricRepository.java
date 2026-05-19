package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.RecommendationMetric;
import com.insightflow.recommendation.enums.RecommendationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface RecommendationMetricRepository extends JpaRepository<RecommendationMetric, UUID> {

    Page<RecommendationMetric> findByMetricDate(LocalDate metricDate, Pageable pageable);

    Page<RecommendationMetric> findByRecommendationType(RecommendationType recommendationType, Pageable pageable);
}
