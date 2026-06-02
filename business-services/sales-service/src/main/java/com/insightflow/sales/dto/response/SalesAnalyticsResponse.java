package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SalesAnalyticsResponse {
    private String period;
    private List<ChannelStatsItem> channelStats;
    private List<LocationStatsItem> locationStats;
}
