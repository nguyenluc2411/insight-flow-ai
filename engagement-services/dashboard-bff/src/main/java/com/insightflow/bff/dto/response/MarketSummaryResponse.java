package com.insightflow.bff.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class MarketSummaryResponse {

    private String period;
    private String location;
    private Kpis kpis;
    private List<ProductOpportunity> productOpportunities;
    private List<ChannelOpportunity> channelOpportunities;
    private List<RegionDemand> regionDemand;
    private List<TrendHighlight> trendHighlights;
    private boolean partial;
    private Instant lastUpdated;

    @Data
    @Builder
    public static class Kpis {
        private Integer opportunityScore;
        private Integer opportunityScoreDelta;
        private Integer potentialProductGroups;
        private String bestChannel;
        private Integer bestChannelScorePct;
        private String competitionLevel;
        private List<String> competitionHotCategories;
    }

    @Data
    @Builder
    public static class ProductOpportunity {
        private String name;
        private String category;
        private String badge;
        private int trendPct;
        private String insight;
        private String imageUrl;
    }

    @Data
    @Builder
    public static class ChannelOpportunity {
        private String channel;
        private int score;
        private int growthPct;
    }

    @Data
    @Builder
    public static class RegionDemand {
        private String region;
        private String demandLevel;
        private int growthPct;
    }

    @Data
    @Builder
    public static class TrendHighlight {
        private String name;
        private String tag;
        private int growthPct;
    }
}
