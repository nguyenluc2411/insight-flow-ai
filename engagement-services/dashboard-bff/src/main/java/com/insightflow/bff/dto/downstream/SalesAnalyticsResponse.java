package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesAnalyticsResponse {

    private String period;
    private List<ChannelStats> channelStats;
    private List<LocationStats> locationStats;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelStats {
        private String channel;
        private long orderCount;
        private BigDecimal totalRevenue;
        private int scorePct;
        private int growthPct;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationStats {
        private UUID locationId;
        private long orderCount;
        private int growthPct;
    }
}
