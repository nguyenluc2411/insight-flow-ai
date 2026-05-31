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

@RestController
@RequestMapping("/api/v1/billing/admin")
@RequiredArgsConstructor
@Tag(name = "Billing Admin", description = "Self-service billing views for the current tenant")
public class BillingAdminController {

    private final BillingHistoryService billingHistoryService;

    @GetMapping("/history")
    @Operation(summary = "Get billing history for the CURRENT tenant")
    @ApiResponse(responseCode = "200", description = "Billing history returned")
    public ResponseEntity<Page<BillingHistory>> getBillingHistory(
            @CurrentUser UserContext user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Scoped to the caller's own tenant — never accepts a tenantId from the client.
        Page<BillingHistory> history = billingHistoryService.getHistory(
                user.tenantId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(history);
    }

    // Cross-tenant history lookup moved to InternalController (service JWT) — it must
    // never be reachable with a plain user JWT (was a tenant-isolation leak).
}
