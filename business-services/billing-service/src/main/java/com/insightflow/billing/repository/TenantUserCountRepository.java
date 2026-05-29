package com.insightflow.billing.repository;

import com.insightflow.billing.entity.TenantUserCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserCountRepository extends JpaRepository<TenantUserCount, UUID> {

    Optional<TenantUserCount> findByTenantId(UUID tenantId);

    @Modifying
    @Query("UPDATE TenantUserCount u SET u.userCount = u.userCount + 1 WHERE u.tenantId = :tenantId")
    int incrementUserCount(UUID tenantId);

    @Modifying
    @Query("UPDATE TenantUserCount u SET u.userCount = CASE WHEN u.userCount > 0 THEN u.userCount - 1 ELSE 0 END WHERE u.tenantId = :tenantId")
    int decrementUserCount(UUID tenantId);
}
