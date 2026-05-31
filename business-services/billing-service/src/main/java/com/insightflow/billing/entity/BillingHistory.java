package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "billing_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "from_package_code", length = 50)
    private String fromPackageCode;

    @Column(name = "to_package_code", length = 50)
    private String toPackageCode;

    @Column(name = "amount_vnd")
    private Integer amountVnd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
