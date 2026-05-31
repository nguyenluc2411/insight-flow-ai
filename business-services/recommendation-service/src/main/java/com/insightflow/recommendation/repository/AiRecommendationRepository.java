package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, String> {
    Optional<AiRecommendation> findByWorkspaceId(String workspaceId);
}