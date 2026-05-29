package com.insightflow.billing.repository;

import com.insightflow.billing.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {

    @Query("SELECT s FROM TenantSubscription s WHERE s.tenantId = :tenantId AND s.status IN ('ACTIVE', 'TRIAL') ORDER BY s.createdAt DESC")
    Optional<TenantSubscription> findActiveOrTrialByTenantId(UUID tenantId);

    List<TenantSubscription> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<TenantSubscription> findByStatus(String status);

    boolean existsByTenantIdAndStatus(UUID tenantId, String status);
}
