package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.request.CreateProductRequest;
import com.insightflow.catalog.dto.request.UpdateProductRequest;
import com.insightflow.catalog.dto.response.ProductResponse;
import com.insightflow.catalog.entity.Category;
import com.insightflow.catalog.entity.Product;
import com.insightflow.catalog.exception.DuplicateResourceException;
import com.insightflow.catalog.exception.ResourceNotFoundException;
import com.insightflow.catalog.mapper.ProductMapper;
import com.insightflow.catalog.repository.CategoryRepository;
import com.insightflow.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

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
        return productRepository.findByTenantId(tenantId, pageable)
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
}
