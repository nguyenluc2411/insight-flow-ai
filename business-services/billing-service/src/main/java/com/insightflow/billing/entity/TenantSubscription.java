package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "tenant_subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "price_at_subscription", nullable = false)
    private Integer priceAtSubscription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_at_subscription", columnDefinition = "jsonb")
    private List<String> featuresAtSubscription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limits_at_subscription", columnDefinition = "jsonb")
    private Map<String, Object> limitsAtSubscription;

    @Column(name = "plan_version", nullable = false)
    private Integer planVersion;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "auto_renew")
    private Boolean autoRenew;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (autoRenew == null) autoRenew = true;
        if (planVersion == null) planVersion = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
