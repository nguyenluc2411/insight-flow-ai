package com.insightflow.billing.controller;

import com.insightflow.billing.dto.response.PackageResponse;
import com.insightflow.billing.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/packages")
@RequiredArgsConstructor
@Tag(name = "Packages", description = "Billing package management")
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    @Operation(summary = "List all active packages")
    @ApiResponse(responseCode = "200", description = "Packages returned successfully")
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllActivePackages());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get package by code")
    @ApiResponse(responseCode = "200", description = "Package found")
    @ApiResponse(responseCode = "404", description = "Package not found")
    public ResponseEntity<PackageResponse> getPackage(@PathVariable String code) {
        return ResponseEntity.ok(packageService.getPackageByCode(code));
    }
}
