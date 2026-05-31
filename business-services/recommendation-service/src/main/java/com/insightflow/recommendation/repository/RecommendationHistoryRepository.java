package com.insightflow.recommendation.repository;

import com.insightflow.recommendation.entity.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {

    boolean existsByWorkspaceIdAndStatus(String workspaceId, String status);

    // Lấy bản ghi tư vấn mới nhất của một cửa hàng
    Optional<RecommendationHistory> findTopByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}