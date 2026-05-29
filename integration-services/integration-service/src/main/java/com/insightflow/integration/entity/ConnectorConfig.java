package com.insightflow.integration.entity;

import com.insightflow.integration.core.ConnectorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "connector_configs", schema = "integration_db")
@Getter
@Setter
public class ConnectorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 50)
    private ConnectorType connectorType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "pending";

    @Column(name = "credentials_encrypted", nullable = false, columnDefinition = "TEXT")
    private String credentialsEncrypted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
