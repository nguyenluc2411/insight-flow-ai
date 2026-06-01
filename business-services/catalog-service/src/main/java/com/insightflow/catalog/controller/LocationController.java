package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.request.CreateLocationRequest;
import com.insightflow.catalog.dto.response.LocationResponse;
import com.insightflow.catalog.service.LocationService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresPermission;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Store and warehouse locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @RequiresPermission("settings:read")
    @Operation(summary = "List active locations")
    @ApiResponse(responseCode = "200", description = "Success")
    public List<LocationResponse> listLocations(@CurrentUser UserContext user) {
        return locationService.getActiveLocations(user.tenantId());
    }

    @PostMapping
    @RequiresPermission("settings:write")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create location")
    @ApiResponse(responseCode = "201", description = "Location created")
    public LocationResponse createLocation(
            @CurrentUser UserContext user,
            @Valid @RequestBody CreateLocationRequest request) {
        return locationService.createLocation(request, user.tenantId());
    }
}
