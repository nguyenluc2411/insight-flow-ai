package com.insightflow.dataingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionJob extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "workspace_id", length = 36, nullable = false, unique = true)
    private String workspaceId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "processed_records", nullable = false)
    private Integer processedRecords;

    @Column(name = "failed_records", nullable = false)
    private Integer failedRecords;

    @Column(name = "error_log", columnDefinition = "JSON")
    private String errorLog;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}