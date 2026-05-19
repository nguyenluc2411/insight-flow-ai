package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.InventoryLevel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, UUID> {

    @EntityGraph(attributePaths = {"variant", "location"})
    List<InventoryLevel> findByTenantIdAndVariantId(UUID tenantId, UUID variantId);

    List<InventoryLevel> findByTenantIdAndLocationId(UUID tenantId, UUID locationId);

    Optional<InventoryLevel> findByVariantIdAndLocationId(UUID variantId, UUID locationId);
}
