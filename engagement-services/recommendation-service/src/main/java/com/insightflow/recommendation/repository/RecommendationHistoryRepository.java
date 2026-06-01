package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.RecommendationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, UUID> {

    Page<RecommendationHistory> findByRecommendationId(UUID recommendationId, Pageable pageable);
}
