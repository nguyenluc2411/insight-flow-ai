package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByTenantIdAndVariantIdOrderByCreatedAtDesc(
            UUID tenantId, UUID variantId, Pageable pageable);

    boolean existsByTenantIdAndReferenceTypeAndReferenceIdAndVariantId(
            UUID tenantId, String referenceType, UUID referenceId, UUID variantId);
}
