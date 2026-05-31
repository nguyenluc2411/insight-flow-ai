package com.insightflow.billing.controller;

import com.insightflow.billing.dto.response.EntitlementResponse;
import com.insightflow.billing.dto.response.FeatureAccessResponse;
import com.insightflow.billing.dto.response.UsageStatusResponse;
import com.insightflow.billing.service.EntitlementService;
import com.insightflow.billing.service.UsageTrackingService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing/entitlements")
@RequiredArgsConstructor
@Tag(name = "Entitlements", description = "Feature entitlement and usage checks")
public class EntitlementController {

    private final EntitlementService entitlementService;
    private final UsageTrackingService usageTrackingService;

    @GetMapping
    @Operation(summary = "Get all entitlements for current tenant")
    @ApiResponse(responseCode = "200", description = "Entitlements returned")
    public ResponseEntity<EntitlementResponse> getEntitlements(@CurrentUser UserContext user) {
        return ResponseEntity.ok(entitlementService.getEntitlements(user.tenantId()));
    }

    @GetMapping("/features/{featureCode}")
    @Operation(summary = "Check if tenant has access to a specific feature")
    @ApiResponse(responseCode = "200", description = "Access check result")
    public ResponseEntity<FeatureAccessResponse> checkFeature(@CurrentUser UserContext user,
                                                               @PathVariable String featureCode) {
        return ResponseEntity.ok(entitlementService.checkFeatureAccess(user.tenantId(), featureCode));
    }

    @GetMapping("/usage")
    @Operation(summary = "Get today's usage statistics")
    @ApiResponse(responseCode = "200", description = "Usage stats returned")
    public ResponseEntity<UsageStatusResponse> getUsage(@CurrentUser UserContext user) {
        return ResponseEntity.ok(usageTrackingService.getUsageStatus(user.tenantId()));
    }
}
