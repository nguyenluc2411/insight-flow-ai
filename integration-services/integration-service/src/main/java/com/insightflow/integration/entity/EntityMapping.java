package com.insightflow.integration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_mappings", schema = "integration_db")
@Getter
@Setter
public class EntityMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "connector_config_id", nullable = false)
    private UUID connectorConfigId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "internal_id", nullable = false)
    private UUID internalId;

    @Column(name = "sync_hash", length = 64)
    private String syncHash;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt = Instant.now();
}
