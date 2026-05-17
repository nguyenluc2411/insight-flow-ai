package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.request.CreateLocationRequest;
import com.insightflow.catalog.dto.response.LocationResponse;
import com.insightflow.catalog.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Store and warehouse locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "List active locations")
    @ApiResponse(responseCode = "200", description = "Success")
    public List<LocationResponse> listLocations(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return locationService.getActiveLocations(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create location")
    @ApiResponse(responseCode = "201", description = "Location created")
    public LocationResponse createLocation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateLocationRequest request) {
        return locationService.createLocation(request, tenantId);
    }
}
