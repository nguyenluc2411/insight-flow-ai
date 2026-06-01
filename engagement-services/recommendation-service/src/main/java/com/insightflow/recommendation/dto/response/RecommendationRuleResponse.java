package com.insightflow.recommendation.dto.response;

import com.insightflow.recommendation.enums.RecommendationType;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
public class RecommendationRuleResponse {

    private UUID id;
    private String ruleCode;
    private String ruleName;
    private RecommendationType recommendationType;
    private Map<String, Object> ruleCondition;
    private int priority;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
