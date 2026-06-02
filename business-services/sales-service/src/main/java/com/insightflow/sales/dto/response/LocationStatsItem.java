package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LocationStatsItem {
    private UUID locationId;
    private long orderCount;
    private int growthPct;
}
