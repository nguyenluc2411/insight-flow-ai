package com.insightflow.recommendation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ManualRecommendationTriggerRequest {

    @NotNull
    private UUID productId;

    private UUID warehouseId;

    @NotBlank
    @Size(max = 500)
    private String triggerReason;

    @PositiveOrZero
    private Integer stockQuantity;

    @PositiveOrZero
    private Double salesVelocity;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double trendScore;

    @AssertTrue(message = "stockQuantity, salesVelocity, and trendScore are required together")
    public boolean isMetricsComplete() {
        boolean any = stockQuantity != null || salesVelocity != null || trendScore != null;
        boolean all = stockQuantity != null && salesVelocity != null && trendScore != null;
        return !any || all;
    }
}

