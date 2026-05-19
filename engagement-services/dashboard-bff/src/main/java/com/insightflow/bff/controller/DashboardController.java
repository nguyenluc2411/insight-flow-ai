package com.insightflow.bff.controller;

import com.insightflow.bff.dto.response.*;
import com.insightflow.bff.service.DashboardAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Tenant-Id header"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            @Parameter(description = "Tenant ID injected by gateway", required = true)
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /overview tenant={}", tenantId);
        return ResponseEntity.ok(aggregationService.getOverview(tenantId));
    }

    @GetMapping("/health-summary")
    @Operation(
            summary = "Get inventory health summary",
            description = "Aggregates slow-moving SKU count, inventory pressure %, and category risk levels from catalog and ml-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health summary (may be partial)"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Tenant-Id header"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<HealthSummaryResponse> getHealthSummary(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /health-summary tenant={}", tenantId);
        return ResponseEntity.ok(aggregationService.getHealthSummary(tenantId));
    }

    @GetMapping("/recommendations-summary")
    @Operation(
            summary = "Get recommendations summary",
            description = "Aggregates ML recommendation counts by action (CLEARANCE, RESTOCK, PROMOTE) and returns top 3 HIGH priority items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations summary (may be partial)"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Tenant-Id header"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<RecommendationsSummaryResponse> getRecommendationsSummary(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /recommendations-summary tenant={}", tenantId);
        return ResponseEntity.ok(aggregationService.getRecommendationsSummary(tenantId));
    }

    @GetMapping("/forecast-summary")
    @Operation(
            summary = "Get forecast summary",
            description = "Runs batch forecast for top 5 variants from ML recommendations and returns 30-day forecast totals with confidence scores."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Forecast summary (may be partial)"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Tenant-Id header"),
            @ApiResponse(responseCode = "503", description = "All downstream services unavailable")
    })
    public ResponseEntity<ForecastSummaryResponse> getForecastSummary(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /forecast-summary tenant={}", tenantId);
        return ResponseEntity.ok(aggregationService.getForecastSummary(tenantId));
    }
}
