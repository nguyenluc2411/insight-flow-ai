package com.insightflow.recommendation.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendationAnalyticsResponse {

    private long totalRecommendations;
    private long clearanceCount;
    private long restockCount;
    private long highRiskCount;
    private BigDecimal averageConfidenceScore;
}

