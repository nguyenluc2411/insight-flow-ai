package com.insightflow.catalog.controller;

import com.insightflow.catalog.dto.request.CreateProductRequest;
import com.insightflow.catalog.dto.request.CreateVariantRequest;
import com.insightflow.catalog.dto.request.UpdateProductRequest;
import com.insightflow.catalog.dto.request.UpdateVariantRequest;
import com.insightflow.catalog.dto.response.ProductResponse;
import com.insightflow.catalog.dto.response.VariantResponse;
import com.insightflow.catalog.service.ProductService;
import com.insightflow.security.CurrentUser;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @CurrentUser UserContext user,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.getProducts(user.tenantId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "409", description = "SKU root already exists")
    public ProductResponse createProduct(
            @CurrentUser UserContext user,
            @Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request, user.tenantId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ProductResponse getProduct(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID id) {
        return productService.getProductById(id, user.tenantId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ProductResponse updateProduct(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(id, request, user.tenantId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete product", description = "Sets status to inactive")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found")
    public void deleteProduct(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID id) {
        productService.deleteProduct(id, user.tenantId());
    }

    @GetMapping("/{productId}/variants")
    @Operation(summary = "List variants for a product")
    @ApiResponse(responseCode = "200", description = "Variant list")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public List<VariantResponse> getVariants(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID productId) {
        return productService.getVariantsByProduct(productId, user.tenantId());
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product variant")
    @ApiResponse(responseCode = "201", description = "Variant created")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "SKU already exists")
    public VariantResponse createVariant(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID productId,
            @Valid @RequestBody CreateVariantRequest request) {
        return productService.createVariant(productId, request, user.tenantId());
    }

    @GetMapping("/{productId}/variants/{variantId}")
    @Operation(summary = "Get variant by ID")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Product or variant not found")
    public VariantResponse getVariant(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID productId,
            @PathVariable java.util.UUID variantId) {
        return productService.getVariantById(productId, variantId, user.tenantId());
    }

    @PutMapping("/{productId}/variants/{variantId}")
    @Operation(summary = "Update product variant", description = "Patch semantics — only provided fields are updated. SKU cannot be changed.")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "Product or variant not found")
    public VariantResponse updateVariant(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID productId,
            @PathVariable java.util.UUID variantId,
            @Valid @RequestBody UpdateVariantRequest request) {
        return productService.updateVariant(productId, variantId, request, user.tenantId());
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete product variant", description = "Sets variant status to inactive")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Product or variant not found")
    public void deleteVariant(
            @CurrentUser UserContext user,
            @PathVariable java.util.UUID productId,
            @PathVariable java.util.UUID variantId) {
        productService.deleteVariant(productId, variantId, user.tenantId());
    }
}
