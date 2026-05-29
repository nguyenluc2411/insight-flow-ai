package com.insightflow.billing.controller;

import com.insightflow.billing.dto.response.InternalSubscriptionResponse;
import com.insightflow.billing.dto.response.RateLimitResponse;
import com.insightflow.billing.entity.TenantUsage;
import com.insightflow.billing.security.ServiceJwtValidator;
import com.insightflow.billing.service.EntitlementService;
import com.insightflow.billing.dto.response.UpgradeRequestResponse;
import com.insightflow.billing.service.PlanLimitService;
import com.insightflow.billing.service.SubscriptionLifecycleService;
import com.insightflow.billing.service.SubscriptionService;
import com.insightflow.billing.service.UpgradeRequestService;
import com.insightflow.billing.service.UsageTrackingService;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Service-to-service internal endpoints (requires service JWT)")
public class InternalController {

    private final ServiceJwtValidator jwtValidator;
    private final PlanLimitService planLimitService;
    private final EntitlementService entitlementService;
    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;
    private final SubscriptionLifecycleService lifecycleService;
    private final UpgradeRequestService upgradeRequestService;

    @GetMapping("/tenants/{tenantId}/limits")
    @Operation(summary = "Get rate limits for a tenant (service-to-service)")
    public ResponseEntity<RateLimitResponse> getLimits(
            @PathVariable UUID tenantId,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(planLimitService.getRateLimitResponse(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/usage/check")
    @Operation(summary = "Count one API call and check the daily quota (service-to-service)")
    public ResponseEntity<RateLimitResponse> checkAndConsume(
            @PathVariable UUID tenantId,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(planLimitService.checkAndConsumeApiCall(tenantId));
    }

    @GetMapping("/upgrade-requests")
    @Operation(summary = "List upgrade requests by status (admin/ops, service JWT)")
    public ResponseEntity<List<UpgradeRequestResponse>> listUpgradeRequests(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(upgradeRequestService.listByStatus(status));
    }

    @PostMapping("/upgrade-requests/{requestId}/approve")
    @Operation(summary = "Approve an upgrade request and switch the tenant's plan (admin/ops)")
    public ResponseEntity<UpgradeRequestResponse> approveUpgradeRequest(
            @PathVariable UUID requestId,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(upgradeRequestService.approve(requestId));
    }

    @PostMapping("/upgrade-requests/{requestId}/reject")
    @Operation(summary = "Reject an upgrade request (admin/ops)")
    public ResponseEntity<UpgradeRequestResponse> rejectUpgradeRequest(
            @PathVariable UUID requestId,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(upgradeRequestService.reject(requestId, reason));
    }

    @PostMapping("/subscriptions/expire-trials")
    @Operation(summary = "Manually run the trial-expiry job (service-to-service / ops)")
    public ResponseEntity<Map<String, Integer>> expireTrials(
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        int downgraded = lifecycleService.expireTrials();
        return ResponseEntity.ok(Map.of("downgraded", downgraded));
    }

    @GetMapping("/tenants/{tenantId}/features")
    @Operation(summary = "Get feature codes for a tenant (service-to-service)")
    public ResponseEntity<List<String>> getFeatures(
            @PathVariable UUID tenantId,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(entitlementService.getFeatureCodes(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/subscription")
    @Operation(summary = "Get subscription details for a tenant (service-to-service)")
    public ResponseEntity<InternalSubscriptionResponse> getSubscription(
            @PathVariable UUID tenantId,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(subscriptionService.getInternalSubscription(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/usage")
    @Operation(summary = "Get today's usage for a tenant (service-to-service)")
    public ResponseEntity<TenantUsage> getUsage(
            @PathVariable UUID tenantId,
            @RequestHeader("Authorization") String authHeader) {
        jwtValidator.validateServiceToken(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(usageTrackingService.getTodayUsage(tenantId));
    }
}
