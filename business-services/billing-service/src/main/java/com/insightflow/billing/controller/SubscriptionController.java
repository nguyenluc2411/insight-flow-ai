package com.insightflow.billing.controller;

import com.insightflow.billing.dto.request.CreateUpgradeRequestRequest;
import com.insightflow.billing.dto.response.SubscriptionResponse;
import com.insightflow.billing.dto.response.UpgradeRequestResponse;
import com.insightflow.billing.service.SubscriptionService;
import com.insightflow.billing.service.UpgradeRequestService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Tenant subscription management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UpgradeRequestService upgradeRequestService;

    @PostMapping("/upgrade-request")
    @Operation(summary = "Submit a manual upgrade request (admin approves it — no payment in MVP)")
    @ApiResponse(responseCode = "200", description = "Upgrade request created (PENDING)")
    public ResponseEntity<UpgradeRequestResponse> requestUpgrade(@CurrentUser UserContext user,
                                                                 @Valid @RequestBody CreateUpgradeRequestRequest req) {
        return ResponseEntity.ok(
                upgradeRequestService.createRequest(user.tenantId(), req.getPackageCode(), req.getBillingCycle()));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current active subscription")
    @ApiResponse(responseCode = "200", description = "Current subscription returned")
    @ApiResponse(responseCode = "404", description = "No active subscription")
    public ResponseEntity<SubscriptionResponse> getCurrent(@CurrentUser UserContext user) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(user.tenantId()));
    }

    // Self-serve /upgrade and /downgrade were removed for the MVP manual-payment
    // model: plan changes happen only via upgrade-request -> ops approval
    // (see UpgradeRequestService + InternalController). The underlying
    // SubscriptionService.upgradePlan/downgradePlan remain for the approval path.
}
