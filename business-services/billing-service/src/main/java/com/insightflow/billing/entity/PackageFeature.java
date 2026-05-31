package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "package_features")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
