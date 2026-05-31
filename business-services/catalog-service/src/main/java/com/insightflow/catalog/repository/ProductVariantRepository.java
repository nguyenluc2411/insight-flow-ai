package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    Optional<ProductVariant> findByTenantIdAndSku(UUID tenantId, String sku);

    Optional<ProductVariant> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT COUNT(v.id) FROM ProductVariant v WHERE v.tenantId = :tenantId AND v.status = 'active'")
    long countActiveByTenantId(UUID tenantId);
}
