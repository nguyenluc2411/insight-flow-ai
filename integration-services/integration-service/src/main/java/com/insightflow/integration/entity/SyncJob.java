package com.insightflow.integration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs", schema = "integration_db")
@Getter
@Setter
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "connector_config_id", nullable = false)
    private UUID connectorConfigId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "sync_type", nullable = false, length = 30)
    private String syncType;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "queued";

    @Column(name = "cursor_value")
    private String cursorValue;

    @Column(name = "records_processed")
    private int recordsProcessed = 0;

    @Column(name = "records_failed")
    private int recordsFailed = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_log", columnDefinition = "jsonb")
    private Map<String, Object> errorLog;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
