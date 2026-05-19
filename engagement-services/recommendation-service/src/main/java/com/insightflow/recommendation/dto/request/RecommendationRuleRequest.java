package com.insightflow.recommendation.dto.request;

import com.insightflow.recommendation.enums.RecommendationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.Map;

@Data
public class RecommendationRuleRequest {

    @NotBlank
    private String ruleCode;

    @NotBlank
    private String ruleName;

    @NotNull
    private RecommendationType recommendationType;

    @NotNull
    private Map<String, Object> ruleCondition;

    @PositiveOrZero
    private int priority = 1;

    private boolean active = true;
}
