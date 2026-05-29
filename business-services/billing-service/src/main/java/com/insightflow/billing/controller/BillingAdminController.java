package com.insightflow.billing.controller;

import com.insightflow.billing.entity.BillingHistory;
import com.insightflow.billing.service.BillingHistoryService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/admin")
@RequiredArgsConstructor
@Tag(name = "Billing Admin", description = "Admin operations for billing management")
public class BillingAdminController {

    private final BillingHistoryService billingHistoryService;

    @GetMapping("/history")
    @Operation(summary = "Get billing history for current tenant")
    @ApiResponse(responseCode = "200", description = "Billing history returned")
    public ResponseEntity<Page<BillingHistory>> getBillingHistory(
            @CurrentUser UserContext user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BillingHistory> history = billingHistoryService.getHistory(
                user.tenantId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(history);
    }

    @GetMapping("/tenants/{tenantId}/history")
    @Operation(summary = "Get billing history for a specific tenant (admin only)")
    @ApiResponse(responseCode = "200", description = "Billing history returned")
    public ResponseEntity<Page<BillingHistory>> getTenantBillingHistory(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BillingHistory> history = billingHistoryService.getHistory(
                tenantId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(history);
    }
}
