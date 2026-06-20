package com.insightflow.billing.controller;

import com.insightflow.billing.dto.request.CreatePackageRequest;
import com.insightflow.billing.dto.request.UpdatePackageRequest;
import com.insightflow.billing.dto.request.UpdatePlanRequest;
import com.insightflow.billing.dto.response.PackageResponse;
import com.insightflow.billing.dto.response.PlanResponse;
import com.insightflow.billing.service.PackageService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Platform super-admin catalog management — change prices, toggle package
 * visibility, create packages. Billing-service runs permitAll + UserContextFilter
 * (no Spring method security), so the SUPER_ADMIN gate is enforced manually here,
 * mirroring {@link AdminMetricsController}.
 */
@RestController
@RequestMapping("/api/v1/billing/admin/catalog")
@RequiredArgsConstructor
@Tag(name = "Billing Admin Catalog", description = "Package & plan management (SUPER_ADMIN)")
public class AdminCatalogController {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final PackageService packageService;

    private void requireSuperAdmin(UserContext user) {
        if (user == null || user.roles() == null || !user.roles().contains(SUPER_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires SUPER_ADMIN role");
        }
    }

    @GetMapping("/packages")
    @Operation(summary = "List ALL packages (including hidden/inactive) with their plans")
    public List<PackageResponse> listAllPackages(@CurrentUser UserContext user) {
        requireSuperAdmin(user);
        return packageService.getAllPackagesAdmin();
    }

    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new package with an initial monthly plan")
    public PackageResponse createPackage(@CurrentUser UserContext user,
                                         @RequestBody CreatePackageRequest req) {
        requireSuperAdmin(user);
        return packageService.createPackage(req.getCode(), req.getName(), req.getDescription(),
                req.getDisplayOrder(), req.getMonthlyPriceVnd(), req.getTrialDays());
    }

    @PatchMapping("/packages/{id}")
    @Operation(summary = "Update package metadata / visibility")
    public PackageResponse updatePackage(@CurrentUser UserContext user,
                                         @PathVariable UUID id,
                                         @RequestBody UpdatePackageRequest req) {
        requireSuperAdmin(user);
        return packageService.updatePackage(id, req.getName(), req.getDescription(),
                req.getDisplayOrder(), req.getStatus());
    }

    @PatchMapping("/plans/{id}")
    @Operation(summary = "Update a plan's price / trial / cycle / status")
    public PlanResponse updatePlan(@CurrentUser UserContext user,
                                   @PathVariable UUID id,
                                   @RequestBody UpdatePlanRequest req) {
        requireSuperAdmin(user);
        return packageService.updatePlan(id, req.getPriceVnd(), req.getTrialDays(),
                req.getBillingCycle(), req.getStatus());
    }
}
