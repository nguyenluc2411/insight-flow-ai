package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.RecommendationRule;
import com.insightflow.recommendation.enums.RecommendationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecommendationRuleRepository extends JpaRepository<RecommendationRule, UUID> {

    Page<RecommendationRule> findByRecommendationType(RecommendationType recommendationType, Pageable pageable);

    Page<RecommendationRule> findByActiveTrue(Pageable pageable);
}
