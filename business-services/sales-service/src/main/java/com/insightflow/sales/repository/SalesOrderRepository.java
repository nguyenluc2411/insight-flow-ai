package com.insightflow.sales.repository;

import com.insightflow.sales.entity.SalesOrder;
import com.insightflow.sales.repository.projection.ChannelAggregation;
import com.insightflow.sales.repository.projection.LocationAggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    Page<SalesOrder> findByTenantId(UUID tenantId, Pageable pageable);

    Page<SalesOrder> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);

    Optional<SalesOrder> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);

    Optional<SalesOrder> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT o.channel AS channel, COUNT(o) AS orderCount, SUM(o.totalAmount) AS totalRevenue " +
           "FROM SalesOrder o " +
           "WHERE o.tenantId = :tenantId AND o.status = 'completed' " +
           "AND o.orderedAt >= :start AND o.orderedAt < :end " +
           "GROUP BY o.channel ORDER BY SUM(o.totalAmount) DESC")
    List<ChannelAggregation> aggregateByChannel(@Param("tenantId") UUID tenantId,
                                                @Param("start") Instant start,
                                                @Param("end") Instant end);

    @Query("SELECT o.locationId AS locationId, COUNT(o) AS orderCount, SUM(o.totalAmount) AS totalRevenue " +
           "FROM SalesOrder o " +
           "WHERE o.tenantId = :tenantId AND o.status = 'completed' " +
           "AND o.orderedAt >= :start AND o.orderedAt < :end " +
           "AND o.locationId IS NOT NULL " +
           "GROUP BY o.locationId ORDER BY SUM(o.totalAmount) DESC")
    List<LocationAggregation> aggregateByLocation(@Param("tenantId") UUID tenantId,
                                                  @Param("start") Instant start,
                                                  @Param("end") Instant end);
}
