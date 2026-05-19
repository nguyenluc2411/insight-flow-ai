package com.insightflow.bff.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthSummaryResponse {

    private Double inventoryPressurePct;
    private Double sellThroughRate;
    private Long slowMovingSKUCount;
    private List<CategoryRisk> categoryRisks;
    private List<ChannelPerformance> channelPerformance;
    private boolean partial;
    private Instant lastUpdated;

    @Data
    @Builder
    public static class CategoryRisk {
        private String category;
        private Long units;
        private String riskLevel;
    }

    @Data
    @Builder
    public static class ChannelPerformance {
        private String channel;
        private Long orders;
        private Double rate;
    }
}
