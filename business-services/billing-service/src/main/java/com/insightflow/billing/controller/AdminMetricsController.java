package com.insightflow.billing.controller;

import com.insightflow.billing.dto.response.BillingMetricsResponse;
import com.insightflow.billing.service.BillingMetricsService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Platform super-admin revenue/subscription metrics. Gated to SUPER_ADMIN by
 * inspecting the gateway-propagated {@link UserContext} roles (billing-service
 * uses permitAll + UserContextFilter rather than Spring authorities).
 */
@RestController
@RequestMapping("/api/v1/billing/admin/metrics")
@RequiredArgsConstructor
@Tag(name = "Billing Admin Metrics", description = "Platform-wide revenue/subscription metrics (SUPER_ADMIN)")
public class AdminMetricsController {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final BillingMetricsService billingMetricsService;

    @GetMapping
    @Operation(summary = "Aggregate revenue & subscription metrics for the admin dashboard")
    public BillingMetricsResponse getMetrics(
            @CurrentUser UserContext user,
            @RequestParam(defaultValue = "12") int months) {
        if (user == null || user.roles() == null || !user.roles().contains(SUPER_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires SUPER_ADMIN role");
        }
        return billingMetricsService.getMetrics(months);
    }
}
