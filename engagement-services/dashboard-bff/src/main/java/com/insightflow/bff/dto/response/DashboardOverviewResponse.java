package com.insightflow.bff.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardOverviewResponse {

    private Long totalSKU;
    private Long ordersToday;
    private Long revenueToday;
    private Long highPriorityAlerts;
    private String mlStatus;
    private boolean partial;
    private Instant lastUpdated;
}
