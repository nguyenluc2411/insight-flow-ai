package com.insightflow.billing.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Aggregate revenue/subscription metrics for the super-admin dashboard.
 * Amounts are in VND.
 */
public record BillingMetricsResponse(
        long activeSubscriptions,
        long trialSubscriptions,
        long mrr,
        long totalRevenue,
        Map<String, Long> subscriptionsByPlan,
        List<MonthlyRevenue> revenueByMonth
) {
    /** One bucket in the revenue time series, e.g. month="2026-06". */
    public record MonthlyRevenue(String month, long amount) {}
}
