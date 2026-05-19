package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.response.CategorySummaryItem;
import com.insightflow.catalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(
            summary = "List categories with product count",
            description = "Returns flat list of categories with active product count, ordered by name.")
    @ApiResponse(responseCode = "200", description = "Category list")
    public List<CategorySummaryItem> getCategories(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return categoryService.getCategories(tenantId);
    }
}
