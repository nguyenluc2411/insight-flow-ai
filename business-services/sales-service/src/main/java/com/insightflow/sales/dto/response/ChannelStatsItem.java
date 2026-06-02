package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChannelStatsItem {
    private String channel;
    private long orderCount;
    private BigDecimal totalRevenue;
    private int scorePct;
    private int growthPct;
}
