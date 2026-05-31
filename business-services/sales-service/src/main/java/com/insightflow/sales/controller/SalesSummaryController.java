package com.insightflow.sales.controller;

import com.insightflow.sales.service.SalesSummaryScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales/summary")
@RequiredArgsConstructor
@Tag(name = "Sales Summary", description = "Materialized view management")
public class SalesSummaryController {

    private final SalesSummaryScheduler scheduler;

    @PostMapping("/refresh")
    @Operation(
        summary = "Trigger summary refresh",
        description = "Manually refreshes the daily_sales_summary materialized view. "
                    + "Scheduled automatically at 02:00 AM Asia/Ho_Chi_Minh — use this for backfill or testing."
    )
    @ApiResponse(responseCode = "200", description = "Refresh completed")
    @ApiResponse(responseCode = "500", description = "Refresh failed — check service logs")
    public ResponseEntity<Map<String, Object>> refresh() {
        long start = System.currentTimeMillis();
        scheduler.refresh();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "refreshedAt", Instant.now().toString(),
                "durationMs", System.currentTimeMillis() - start
        ));
    }
}
