package com.insightflow.sales.controller;

import com.insightflow.sales.dto.request.CreateSupplierRequest;
import com.insightflow.sales.dto.response.SupplierResponse;
import com.insightflow.sales.service.SupplierService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresPermission;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier management")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @RequiresPermission("sales:read")
    @Operation(summary = "List suppliers")
    @ApiResponse(responseCode = "200", description = "Success")
    public Page<SupplierResponse> listSuppliers(
            @CurrentUser UserContext user,
            @PageableDefault(size = 20) Pageable pageable) {
        return supplierService.getSuppliers(user.tenantId(), pageable);
    }

    @PostMapping
    @RequiresPermission("sales:write")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create supplier")
    @ApiResponse(responseCode = "201", description = "Supplier created")
    public SupplierResponse createSupplier(
            @CurrentUser UserContext user,
            @Valid @RequestBody CreateSupplierRequest request) {
        return supplierService.createSupplier(request, user.tenantId());
    }
}
