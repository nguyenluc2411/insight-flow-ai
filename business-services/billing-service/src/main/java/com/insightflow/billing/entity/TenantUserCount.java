package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "tenant_user_count")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUserCount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "user_count")
    private Integer userCount;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        lastUpdatedAt = LocalDateTime.now();
        if (userCount == null) userCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
