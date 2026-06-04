-- =============================================================================
-- V1 — Workspace + uploaded-file metadata.
-- A workspace is a single file-upload/analysis session owned by a tenant.
-- All rows carry tenant_id; every query is tenant-scoped for isolation.
-- =============================================================================
SET search_path TO workspace_db;

CREATE TABLE workspaces (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(50),                 -- creator within the tenant (metadata)
    name          VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL,        -- INIT, PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    progress      INTEGER,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_workspaces_tenant ON workspaces(tenant_id);

CREATE TABLE file_metadata (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    workspace_id  VARCHAR(36)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    content_type  VARCHAR(50)  NOT NULL,
    s3_file_url   VARCHAR(500) NOT NULL,
    uploaded_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_file_metadata_workspace UNIQUE (workspace_id),
    CONSTRAINT fk_file_metadata_workspace FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX idx_file_metadata_tenant ON file_metadata(tenant_id);
