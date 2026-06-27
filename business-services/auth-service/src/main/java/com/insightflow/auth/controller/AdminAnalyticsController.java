package com.insightflow.auth.controller;

import com.insightflow.auth.dto.AdminFunnelResponse;
import com.insightflow.auth.service.AnalyticsService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresRole;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Analytics", description = "Platform analytics for super admin")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/funnel")
    @RequiresRole("SUPER_ADMIN")
    @Operation(summary = "Get conversion funnel data")
    public ResponseEntity<AdminFunnelResponse> getFunnelAnalytics(
            @CurrentUser UserContext user,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        return ResponseEntity.ok(analyticsService.getFunnelAnalytics(fromDate, toDate));
    }
}
