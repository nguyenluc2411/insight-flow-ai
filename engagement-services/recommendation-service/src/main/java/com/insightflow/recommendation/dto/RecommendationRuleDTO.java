package com.insightflow.recommendation.dto;

import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RuleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class RecommendationRuleDTO {

    @NotNull
    private RuleType ruleType;

    @PositiveOrZero
    private Integer minStock;

    @PositiveOrZero
    private Integer maxStock;

    @PositiveOrZero
    private Double minSalesVelocity;

    @PositiveOrZero
    private Double maxSalesVelocity;

    @NotNull
    private RecommendationType actionType;

    private boolean enabled = true;
}
