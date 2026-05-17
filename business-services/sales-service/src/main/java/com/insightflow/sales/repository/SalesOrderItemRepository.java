package com.insightflow.sales.repository;

import com.insightflow.sales.entity.SalesOrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {

    List<SalesOrderItem> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    Page<SalesOrderItem> findByTenantIdAndVariantId(UUID tenantId, UUID variantId, Pageable pageable);
}
