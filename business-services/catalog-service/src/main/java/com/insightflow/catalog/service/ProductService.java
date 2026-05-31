package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.request.CreateProductRequest;
import com.insightflow.catalog.dto.request.CreateVariantRequest;
import com.insightflow.catalog.dto.request.UpdateProductRequest;
import com.insightflow.catalog.dto.request.UpdateVariantRequest;
import com.insightflow.catalog.dto.response.ProductResponse;
import com.insightflow.catalog.dto.response.VariantResponse;
import com.insightflow.catalog.entity.Category;
import com.insightflow.catalog.entity.Product;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.catalog.mapper.ProductMapper;
import com.insightflow.catalog.mapper.VariantMapper;
import com.insightflow.catalog.repository.CategoryRepository;
import com.insightflow.catalog.repository.ProductRepository;
import com.insightflow.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMapper productMapper;
    private final VariantMapper variantMapper;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, UUID tenantId) {
        if (productRepository.findByTenantIdAndSkuRoot(tenantId, request.getSkuRoot()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Product with skuRoot already exists: " + request.getSkuRoot());
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setSkuRoot(request.getSkuRoot());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setSeason(request.getSeason());
        product.setGender(request.getGender());
        product.setTags(request.getTags());
        product.setStatus("active");

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByTenantIdAndId(tenantId, request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        log.debug("Created product id={} tenantId={}", saved.getId(), tenantId);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (request.getName() != null)        product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getBrand() != null)       product.setBrand(request.getBrand());
        if (request.getSeason() != null)      product.setSeason(request.getSeason());
        if (request.getGender() != null)      product.setGender(request.getGender());
        if (request.getTags() != null)        product.setTags(request.getTags());
        if (request.getStatus() != null)      product.setStatus(request.getStatus());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByTenantIdAndId(tenantId, request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            product.setCategory(category);
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(UUID tenantId, Pageable pageable) {
        return productRepository.findByTenantIdAndStatus(tenantId, "active", pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id, UUID tenantId) {
        return productRepository.findByTenantIdAndId(tenantId, id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    public void deleteProduct(UUID id, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setStatus("inactive");
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> getVariantsByProduct(UUID productId, UUID tenantId) {
        // Verify product belongs to tenant before returning variants
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return variantRepository.findByTenantIdAndProductId(tenantId, productId)
                .stream()
                .map(variantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse getVariantById(UUID productId, UUID variantId, UUID tenantId) {
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return variantRepository.findByTenantIdAndId(tenantId, variantId)
                .map(variantMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
    }

    @Transactional
    public VariantResponse updateVariant(UUID productId, UUID variantId, UpdateVariantRequest request, UUID tenantId) {
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        ProductVariant variant = variantRepository.findByTenantIdAndId(tenantId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        if (request.getBarcode() != null)        variant.setBarcode(request.getBarcode());
        if (request.getSize() != null)           variant.setSize(request.getSize());
        if (request.getColor() != null)          variant.setColor(request.getColor());
        if (request.getColorHex() != null)       variant.setColorHex(request.getColorHex());
        if (request.getCostPrice() != null)      variant.setCostPrice(request.getCostPrice());
        if (request.getSellingPrice() != null)   variant.setSellingPrice(request.getSellingPrice());
        if (request.getCompareAtPrice() != null) variant.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getStatus() != null)         variant.setStatus(request.getStatus());

        log.debug("Updated variant id={} productId={} tenantId={}", variantId, productId, tenantId);
        return variantMapper.toResponse(variantRepository.save(variant));
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID variantId, UUID tenantId) {
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        ProductVariant variant = variantRepository.findByTenantIdAndId(tenantId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        variant.setStatus("inactive");
        variantRepository.save(variant);
        log.debug("Soft-deleted variant id={} productId={} tenantId={}", variantId, productId, tenantId);
    }

    @Transactional
    public VariantResponse createVariant(UUID productId, CreateVariantRequest request, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (variantRepository.findByTenantIdAndSku(tenantId, request.getSku()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Variant with SKU already exists: " + request.getSku());
        }

        ProductVariant variant = new ProductVariant();
        variant.setTenantId(tenantId);
        variant.setProduct(product);
        variant.setSku(request.getSku());
        variant.setBarcode(request.getBarcode());
        variant.setSize(request.getSize());
        variant.setColor(request.getColor());
        variant.setColorHex(request.getColorHex());
        variant.setCostPrice(request.getCostPrice());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getStatus() != null) {
            variant.setStatus(request.getStatus());
        }

        ProductVariant saved = variantRepository.save(variant);
        log.debug("Created variant id={} productId={} tenantId={}", saved.getId(), productId, tenantId);
        return variantMapper.toResponse(saved);
    }
}
