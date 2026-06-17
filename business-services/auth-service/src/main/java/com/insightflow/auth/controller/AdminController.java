package com.insightflow.auth.controller;

import com.insightflow.auth.dto.request.UpdateTenantStatusRequest;
import com.insightflow.auth.dto.response.AdminTenantDetail;
import com.insightflow.auth.dto.response.AdminTenantListItem;
import com.insightflow.auth.service.AdminTenantService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresRole;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Platform super-admin endpoints. Every method requires the SUPER_ADMIN role
 * (enforced by {@link RequiresRole}); the gateway provides a first line of
 * defense by also gating {@code /api/v1/admin/**}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Platform super-admin operations")
public class AdminController {

    private final AdminTenantService adminTenantService;

    @GetMapping("/tenants")
    @RequiresRole("SUPER_ADMIN")
    @Operation(summary = "List all tenants (paginated, filter by status / search by name|slug)")
    public Page<AdminTenantListItem> listTenants(
            @CurrentUser UserContext user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return adminTenantService.listTenants(status, q, pageable);
    }

    @GetMapping("/tenants/{id}")
    @RequiresRole("SUPER_ADMIN")
    @Operation(summary = "Get a tenant's full detail including its users")
    public AdminTenantDetail getTenant(@CurrentUser UserContext user, @PathVariable UUID id) {
        return adminTenantService.getTenant(id);
    }

    @PatchMapping("/tenants/{id}/status")
    @RequiresRole("SUPER_ADMIN")
    @Operation(summary = "Suspend or re-activate a tenant")
    public AdminTenantDetail updateTenantStatus(
            @CurrentUser UserContext user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantStatusRequest request) {
        return adminTenantService.updateStatus(id, request.getStatus());
    }
}
