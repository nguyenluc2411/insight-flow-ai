package com.insightflow.sales.controller;

import com.insightflow.sales.dto.response.SalesAnalyticsResponse;
import com.insightflow.sales.service.SalesAnalyticsService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresPermission;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales/analytics")
@RequiredArgsConstructor
@Tag(name = "Sales Analytics", description = "Aggregated sales analytics for market intelligence")
public class SalesAnalyticsController {

    private final SalesAnalyticsService analyticsService;

    @GetMapping("/summary")
    @RequiresPermission("sales:read")
    @Operation(
        summary = "Get channel and location sales summary",
        description = "Returns revenue/order aggregates by channel and location for a given quarter. " +
                      "period format: 2026-Q2. Used by dashboard-bff for market-summary aggregation."
    )
    public SalesAnalyticsResponse getSummary(
            @CurrentUser UserContext user,
            @RequestParam(required = false) String period) {
        String resolvedPeriod = period != null ? period : currentQuarter();
        return analyticsService.getAnalytics(user.tenantId(), resolvedPeriod);
    }

    private static String currentQuarter() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int q = (now.getMonthValue() - 1) / 3 + 1;
        return now.getYear() + "-Q" + q;
    }
}
