package com.insightflow.sales.repository;

import com.insightflow.sales.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    Page<SalesOrder> findByTenantId(UUID tenantId, Pageable pageable);

    Page<SalesOrder> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);

    Optional<SalesOrder> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);

    Optional<SalesOrder> findByTenantIdAndId(UUID tenantId, UUID id);
}
