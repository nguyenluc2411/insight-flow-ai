package com.insightflow.userworkspace.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRecommendationGeneratedPayload {
    @JsonProperty("workspace_id")
    private String workspaceId;
    @JsonProperty("recommendation_text")
    private String recommendationText;
    @JsonProperty("confidence_score")
    private Double confidenceScore;
    @JsonProperty("processed_at")
    private String processedAt;
}