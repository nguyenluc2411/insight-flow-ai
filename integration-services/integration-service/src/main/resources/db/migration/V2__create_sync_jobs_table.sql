CREATE TABLE integration_db.sync_jobs (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID         NOT NULL,
    connector_config_id  UUID         NOT NULL
        REFERENCES integration_db.connector_configs (id),
    entity_type          VARCHAR(50)  NOT NULL,
    sync_type            VARCHAR(30)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'queued',
    cursor_value         TEXT,
    records_processed    INTEGER      NOT NULL DEFAULT 0,
    records_failed       INTEGER      NOT NULL DEFAULT 0,
    error_log            JSONB,
    started_at           TIMESTAMP WITH TIME ZONE,
    completed_at         TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_jobs_tenant
    ON integration_db.sync_jobs (tenant_id, created_at DESC);

CREATE INDEX idx_sync_jobs_connector
    ON integration_db.sync_jobs (connector_config_id);
