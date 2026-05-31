package com.insightflow.recommendation.service;

import com.insightflow.recommendation.dto.event.InventoryIngestionCompletedPayload;
import com.insightflow.recommendation.entity.RecommendationHistory;

public interface RecommendationService {
    void processRecommendation(InventoryIngestionCompletedPayload payload);
    RecommendationHistory getRecommendationByWorkspace(String workspaceId);
}