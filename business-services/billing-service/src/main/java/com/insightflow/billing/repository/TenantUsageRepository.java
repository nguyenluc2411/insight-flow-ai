package com.insightflow.billing.repository;

import com.insightflow.billing.entity.TenantUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUsageRepository extends JpaRepository<TenantUsage, UUID> {

    Optional<TenantUsage> findByTenantIdAndUsageDate(UUID tenantId, LocalDate usageDate);

    List<TenantUsage> findByTenantIdOrderByUsageDateDesc(UUID tenantId);

    @Modifying
    @Query("UPDATE TenantUsage u SET u.apiCallsCount = u.apiCallsCount + 1 WHERE u.tenantId = :tenantId AND u.usageDate = :date")
    int incrementApiCalls(UUID tenantId, LocalDate date);

    @Modifying
    @Query("UPDATE TenantUsage u SET u.productExportsCount = u.productExportsCount + 1 WHERE u.tenantId = :tenantId AND u.usageDate = :date")
    int incrementProductExports(UUID tenantId, LocalDate date);

    @Modifying
    @Query("UPDATE TenantUsage u SET u.reportsGeneratedCount = u.reportsGeneratedCount + 1 WHERE u.tenantId = :tenantId AND u.usageDate = :date")
    int incrementReportsGenerated(UUID tenantId, LocalDate date);

    @Modifying
    @Query("UPDATE TenantUsage u SET u.forecastsExecutedCount = u.forecastsExecutedCount + 1 WHERE u.tenantId = :tenantId AND u.usageDate = :date")
    int incrementForecastsExecuted(UUID tenantId, LocalDate date);
}
