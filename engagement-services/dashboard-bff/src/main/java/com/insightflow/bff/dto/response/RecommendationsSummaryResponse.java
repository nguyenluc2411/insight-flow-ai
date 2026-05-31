package com.insightflow.bff.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationsSummaryResponse {

    private Long total;
    private Map<String, Long> byAction;
    private List<TopAction> topActions;
    private EstimatedImpact estimatedImpact;
    private boolean partial;
    private Instant lastUpdated;

    @Data
    @Builder
    public static class TopAction {
        private UUID variantId;
        private String action;
        private String priority;
        private String reason;
        private Double suggestedDiscountPct;
        private Integer suggestedRestockQty;
        private Integer stockAgeDays;
        private Integer currentStock;
    }

    @Data
    @Builder
    public static class EstimatedImpact {
        private Long clearanceItems;
        private Long restockItems;
        private Long promoteItems;
        private Double avgDiscountPct;
    }
}
