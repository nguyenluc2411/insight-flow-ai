package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Product> findByTenantIdAndSkuRoot(UUID tenantId, String skuRoot);

    Optional<Product> findByTenantIdAndId(UUID tenantId, UUID id);
}
