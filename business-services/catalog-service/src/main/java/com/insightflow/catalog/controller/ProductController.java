package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.request.CreateProductRequest;
import com.insightflow.catalog.dto.request.CreateVariantRequest;
import com.insightflow.catalog.dto.request.UpdateProductRequest;
import com.insightflow.catalog.dto.response.ProductResponse;
import com.insightflow.catalog.dto.response.VariantResponse;
import com.insightflow.catalog.service.ProductService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog management")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products", description = "Paginated product list for the tenant")
    @ApiResponse(responseCode = "200", description = "Success")
    public Page<ProductResponse> listProducts(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.getProducts(tenantId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "409", description = "SKU root already exists")
    public ProductResponse createProduct(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request, tenantId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ProductResponse getProduct(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        return productService.getProductById(id, tenantId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ProductResponse updateProduct(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(id, request, tenantId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete product", description = "Sets status to inactive")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found")
    public void deleteProduct(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        productService.deleteProduct(id, tenantId);
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product variant")
    @ApiResponse(responseCode = "201", description = "Variant created")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "SKU already exists")
    public VariantResponse createVariant(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateVariantRequest request) {
        return productService.createVariant(productId, request, tenantId);
    }
}
