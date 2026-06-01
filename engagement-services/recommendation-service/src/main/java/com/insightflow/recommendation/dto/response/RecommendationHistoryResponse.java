package com.insightflow.recommendation.dto.response;

import com.insightflow.recommendation.enums.RecommendationStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RecommendationHistoryResponse {

    private UUID id;
    private UUID recommendationId;
    private RecommendationStatus previousStatus;
    private RecommendationStatus newStatus;
    private String changeReason;
    private String changedBy;
    private Instant changedAt;
}

