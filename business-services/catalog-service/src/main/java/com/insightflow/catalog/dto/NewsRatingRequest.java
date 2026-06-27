package com.insightflow.catalog.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class NewsRatingRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer ratingValue;
}
