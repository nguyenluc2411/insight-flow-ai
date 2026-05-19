package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.request.RecordMovementRequest;
import com.insightflow.catalog.dto.response.InventoryLevelResponse;
import com.insightflow.catalog.dto.response.InventoryMovementResponse;
import com.insightflow.catalog.dto.response.InventorySummaryResponse;
import com.insightflow.catalog.service.InventoryService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory levels and movement history")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/summary")
    @Operation(
            summary = "Inventory summary",
            description = "Returns totalSKU (active variants), totalQuantity (sum on-hand), lowStockCount (positions at/below reorder threshold).")
    @ApiResponse(responseCode = "200", description = "Summary")
    public InventorySummaryResponse getSummary(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return inventoryService.getSummary(tenantId);
    }

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Get inventory levels by variant")
    @ApiResponse(responseCode = "200", description = "Success")
    public List<InventoryLevelResponse> getLevelsByVariant(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID variantId) {
        return inventoryService.getInventoryByVariant(variantId, tenantId);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record inventory movement",
               description = "Appends a movement record and updates inventory level. Publishes catalog.inventory.updated to Kafka.")
    @ApiResponse(responseCode = "201", description = "Movement recorded")
    @ApiResponse(responseCode = "404", description = "Variant or location not found")
    public InventoryMovementResponse recordMovement(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RecordMovementRequest request) {
        return inventoryService.recordMovement(request, tenantId);
    }

    @GetMapping("/movements/{variantId}")
    @Operation(summary = "Get movement history by variant", description = "Ordered by createdAt DESC")
    @ApiResponse(responseCode = "200", description = "Success")
    public Page<InventoryMovementResponse> getMovementHistory(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID variantId,
            @PageableDefault(size = 50) Pageable pageable) {
        return inventoryService.getMovementHistory(variantId, tenantId, pageable);
    }
}
