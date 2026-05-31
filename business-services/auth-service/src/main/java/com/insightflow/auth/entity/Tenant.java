package com.insightflow.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "auth_db", name = "tenants")
@Getter @Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String plan = "trial";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    private Instant trialEndsAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> settings = Map.of();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
