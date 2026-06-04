package com.insightflow.bff.controller;

import com.insightflow.bff.dto.response.*;
import com.insightflow.bff.service.DashboardAggregationService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard BFF", description = "Aggregated dashboard data from catalog, sales, and ml-service")
public class DashboardController {

    private final DashboardAggregationService aggregationService;

    @GetMapping("/overview")
    @Operation(
            summary = "Get dashboard overview",
            description = "Aggregates total SKU count, today's orders/revenue, high-priority ML alerts, and ML service status. Returns partial=true if any downstream service is unavailable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview data (may be partial)"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<DashboardOverviewResponse> getOverview(@CurrentUser UserContext user) {
        log.debug("GET /overview tenant={}", user.tenantId());
        return ResponseEntity.ok(aggregationService.getOverview(user));
    }

    @GetMapping("/health-summary")
    @Operation(
            summary = "Get inventory health summary",
            description = "Aggregates slow-moving SKU count, inventory pressure %, and category risk levels from catalog and ml-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health summary (may be partial)"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<HealthSummaryResponse> getHealthSummary(@CurrentUser UserContext user) {
        log.debug("GET /health-summary tenant={}", user.tenantId());
        return ResponseEntity.ok(aggregationService.getHealthSummary(user));
    }

    @GetMapping("/recommendations-summary")
    @Operation(
            summary = "Get recommendations summary",
            description = "Aggregates ML recommendation counts by action (CLEARANCE, RESTOCK, PROMOTE) and returns top 3 HIGH priority items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations summary (may be partial)"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<RecommendationsSummaryResponse> getRecommendationsSummary(@CurrentUser UserContext user) {
        log.debug("GET /recommendations-summary tenant={}", user.tenantId());
        return ResponseEntity.ok(aggregationService.getRecommendationsSummary(user));
    }

    @GetMapping("/forecast-summary")
    @Operation(
            summary = "Get forecast summary",
            description = "Runs batch forecast for top 5 variants from ML recommendations and returns 30-day forecast totals with confidence scores."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Forecast summary (may be partial)"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<ForecastSummaryResponse> getForecastSummary(@CurrentUser UserContext user) {
        log.debug("GET /forecast-summary tenant={}", user.tenantId());
        return ResponseEntity.ok(aggregationService.getForecastSummary(user));
    }

    @GetMapping("/market-summary")
    @Cacheable(value = "market-summary",
               key = "#user.tenantId().toString() + '-' + #location + '-' + #period")
    @Operation(
            summary = "Get market opportunity summary",
            description = "Aggregates channel performance (sales-service), region demand, " +
                          "ML restock/clearance recommendations, and Google Trends fashion data. " +
                          "Cached 5 min per tenant/location/period. partial=true when trends unavailable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Market summary (may be partial)"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<MarketSummaryResponse> getMarketSummary(
            @CurrentUser UserContext user,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period) {
        log.debug("GET /market-summary tenant={} location={} period={}", user.tenantId(), location, period);
        return ResponseEntity.ok(aggregationService.getMarketSummary(user, location, period));
    }
}
