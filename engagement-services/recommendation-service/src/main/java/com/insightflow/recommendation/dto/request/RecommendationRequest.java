package com.insightflow.recommendation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.UUID;

@Data
public class RecommendationRequest {

    @NotNull
    private UUID productId;

    private UUID warehouseId;

    @NotNull
    private UUID eventId;

    @NotNull
    @PositiveOrZero
    private Integer stockQuantity;

    @NotNull
    @PositiveOrZero
    private Double salesVelocity;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double trendScore;
}
