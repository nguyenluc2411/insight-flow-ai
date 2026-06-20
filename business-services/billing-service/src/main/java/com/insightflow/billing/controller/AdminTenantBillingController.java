package com.insightflow.billing.controller;

import com.insightflow.billing.entity.BillingHistory;
import com.insightflow.billing.entity.PaymentTransaction;
import com.insightflow.billing.repository.PaymentTransactionRepository;
import com.insightflow.billing.service.BillingHistoryService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Platform super-admin: inspect ANY tenant's billing history & payment
 * transactions. SUPER_ADMIN is enforced manually (billing runs permitAll +
 * UserContextFilter). Distinct from {@code BillingAdminController}, which is
 * self-service and scoped to the caller's own tenant.
 */
@RestController
@RequestMapping("/api/v1/billing/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Billing Admin Tenants", description = "Per-tenant billing history & transactions (SUPER_ADMIN)")
public class AdminTenantBillingController {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final BillingHistoryService billingHistoryService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    private void requireSuperAdmin(UserContext user) {
        if (user == null || user.roles() == null || !user.roles().contains(SUPER_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires SUPER_ADMIN role");
        }
    }

    @GetMapping("/{tenantId}/history")
    @Operation(summary = "Billing history (subscription events) for a given tenant")
    public Page<BillingHistory> getHistory(@CurrentUser UserContext user,
                                           @PathVariable UUID tenantId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        requireSuperAdmin(user);
        return billingHistoryService.getHistory(tenantId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @GetMapping("/{tenantId}/transactions")
    @Operation(summary = "Payment transactions (SePay) for a given tenant")
    public Page<PaymentTransaction> getTransactions(@CurrentUser UserContext user,
                                                    @PathVariable UUID tenantId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        requireSuperAdmin(user);
        return paymentTransactionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId,
                PageRequest.of(page, size));
    }
}
