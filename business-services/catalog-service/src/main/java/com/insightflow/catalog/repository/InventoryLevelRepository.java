package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.InventoryLevel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /** Total units on hand across all locations for the tenant. */
    @Query("SELECT COALESCE(SUM(il.quantityOnHand), 0) FROM InventoryLevel il WHERE il.tenantId = :tenantId")
    long sumQuantityOnHand(UUID tenantId);

    /**
     * Count of stock positions (variant × location) at or below their reorder threshold.
     * Uses reorder_point when set, falls back to 10 units as default threshold.
     * quantityOnHand = 0 is excluded (already out-of-stock, tracked separately).
     */
    @Query("""
            SELECT COUNT(il.id)
            FROM InventoryLevel il
            WHERE il.tenantId = :tenantId
              AND il.quantityOnHand > 0
              AND il.quantityOnHand <= COALESCE(il.reorderPoint, 10)
            """)
    long countLowStock(UUID tenantId);
}
