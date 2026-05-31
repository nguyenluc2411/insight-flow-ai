package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "tenant_usage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "api_calls_count")
    private Integer apiCallsCount;

    @Column(name = "product_exports_count")
    private Integer productExportsCount;

    @Column(name = "reports_generated_count")
    private Integer reportsGeneratedCount;

    @Column(name = "forecasts_executed_count")
    private Integer forecastsExecutedCount;

    @Column(name = "storage_used_bytes")
    private Long storageUsedBytes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (apiCallsCount == null) apiCallsCount = 0;
        if (productExportsCount == null) productExportsCount = 0;
        if (reportsGeneratedCount == null) reportsGeneratedCount = 0;
        if (forecastsExecutedCount == null) forecastsExecutedCount = 0;
        if (storageUsedBytes == null) storageUsedBytes = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
