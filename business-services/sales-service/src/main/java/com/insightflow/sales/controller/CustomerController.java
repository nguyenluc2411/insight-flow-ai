package com.insightflow.sales.controller;

import com.insightflow.sales.dto.request.CreateCustomerRequest;
import com.insightflow.sales.dto.response.CustomerResponse;
import com.insightflow.sales.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/sales/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List customers")
    @ApiResponse(responseCode = "200", description = "Success")
    public Page<CustomerResponse> listCustomers(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        return customerService.getCustomers(tenantId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create customer")
    @ApiResponse(responseCode = "201", description = "Customer created")
    @ApiResponse(responseCode = "409", description = "Phone already registered")
    public CustomerResponse createCustomer(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(request, tenantId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not found")
    public CustomerResponse getCustomer(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        return customerService.getCustomerById(id, tenantId);
    }
}
