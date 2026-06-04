package com.insightflow.userworkspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    // Owning tenant — every query is scoped by this for multi-tenant isolation.
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    // Creator (user) within the tenant — kept as metadata, not used for isolation.
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // INIT, PROCESSING, COMPLETED, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "progress")
    private Integer progress;
}