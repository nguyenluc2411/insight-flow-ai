package com.insightflow.sales.repository;

import com.insightflow.sales.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Page<Supplier> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Supplier> findByTenantIdAndId(UUID tenantId, UUID id);
}
