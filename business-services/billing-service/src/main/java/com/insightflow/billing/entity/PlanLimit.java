package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "plan_limits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "package_id", nullable = false, unique = true)
    private UUID packageId;

    @Column(name = "max_api_calls_per_day")
    private Integer maxApiCallsPerDay;

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "api_rate_limit_per_minute")
    private Integer apiRateLimitPerMinute;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
