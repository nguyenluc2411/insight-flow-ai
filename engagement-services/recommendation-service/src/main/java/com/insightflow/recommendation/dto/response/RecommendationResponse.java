package com.insightflow.recommendation.dto.response;

import com.insightflow.recommendation.enums.RecommendationStatus;
import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RecommendationPriority;
import com.insightflow.recommendation.enums.RiskLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class RecommendationResponse {

    private UUID id;
    private UUID productId;
    private UUID warehouseId;
    private RecommendationType recommendationType;
    private RecommendationStatus status;
    private RecommendationPriority priority;
    private RiskLevel riskLevel;
    private BigDecimal confidenceScore;
    private String recommendationReason;
    private Instant generatedAt;
    private Instant expiresAt;
}
