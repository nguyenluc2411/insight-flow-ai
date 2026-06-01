package com.insightflow.bff.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForecastSummaryResponse {

    private List<CategoryTrend> categoryTrends;
    private List<TopProduct> topProducts;
    private Double overallConfidence;
    private boolean partial;
    private Boolean hasColdStart;
    private String message;
    private Instant lastUpdated;

    @Data
    @Builder
    public static class CategoryTrend {
        private String category;
        private String trend;
        private Double pct;
    }

    @Data
    @Builder
    public static class TopProduct {
        private UUID variantId;
        private String sku;
        private Double forecastDays30;
        private String confidence;
    }
}
