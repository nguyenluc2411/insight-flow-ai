package com.insightflow.recommendation.service;

import com.insightflow.recommendation.dto.request.ManualRecommendationTriggerRequest;
import com.insightflow.recommendation.dto.request.RecommendationFilterRequest;
import com.insightflow.recommendation.dto.request.RecommendationRequest;
import com.insightflow.recommendation.dto.response.RecommendationAnalyticsResponse;
import com.insightflow.recommendation.dto.response.RecommendationHistoryResponse;
import com.insightflow.recommendation.dto.response.RecommendationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecommendationService {

    RecommendationResponse createRecommendation(RecommendationRequest request);

    RecommendationRuleEngine.RecommendationDecision evaluateInventoryState(RecommendationRequest request);

    Page<RecommendationResponse> fetchRecommendations(Pageable pageable);

    RecommendationResponse refreshRecommendation(UUID recommendationId, RecommendationRequest request);

    Page<RecommendationResponse> getRecommendations(RecommendationFilterRequest filter);

    RecommendationResponse getRecommendation(UUID recommendationId);

    Page<RecommendationHistoryResponse> getRecommendationHistory(UUID recommendationId, Pageable pageable);

    RecommendationAnalyticsResponse getRecommendationAnalytics();

    RecommendationResponse triggerManualRecommendation(ManualRecommendationTriggerRequest request);
}
