package com.insightflow.auth.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Aggregate platform metrics for the super-admin dashboard (tenant + user side).
 * Revenue/subscription metrics are served separately by billing-service.
 */
public record AdminMetricsResponse(
        long totalTenants,
        long activeTenants,
        long trialTenants,
        long suspendedTenants,
        long totalUsers,
        Map<String, Long> tenantsByStatus,
        Map<String, Long> tenantsByPlan,
        List<DailyCount> newTenantsByDay
) {
    /** One day in the sign-up time series. */
    public record DailyCount(LocalDate date, long count) {}
}
