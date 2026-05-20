package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.request.CreateProductRequest;
import com.insightflow.catalog.dto.request.CreateVariantRequest;
import com.insightflow.catalog.dto.request.UpdateProductRequest;
import com.insightflow.catalog.dto.response.ProductResponse;
import com.insightflow.catalog.dto.response.VariantResponse;
import com.insightflow.catalog.entity.Category;
import com.insightflow.catalog.entity.Product;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.catalog.exception.DuplicateResourceException;
import com.insightflow.catalog.exception.ResourceNotFoundException;
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
            throw new DuplicateResourceException("Product with skuRoot already exists: " + request.getSkuRoot());
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
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        log.debug("Created product id={} tenantId={}", saved.getId(), tenantId);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getName() != null)        product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getBrand() != null)       product.setBrand(request.getBrand());
        if (request.getSeason() != null)      product.setSeason(request.getSeason());
        if (request.getGender() != null)      product.setGender(request.getGender());
        if (request.getTags() != null)        product.setTags(request.getTags());
        if (request.getStatus() != null)      product.setStatus(request.getStatus());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
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
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional
    public void deleteProduct(UUID id, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setStatus("inactive");
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> getVariantsByProduct(UUID productId, UUID tenantId) {
        // Verify product belongs to tenant before returning variants
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return variantRepository.findByTenantIdAndProductId(tenantId, productId)
                .stream()
                .map(variantMapper::toResponse)
                .toList();
    }

    @Transactional
    public VariantResponse createVariant(UUID productId, CreateVariantRequest request, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (variantRepository.findByTenantIdAndSku(tenantId, request.getSku()).isPresent()) {
            throw new DuplicateResourceException("Variant with SKU already exists: " + request.getSku());
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
