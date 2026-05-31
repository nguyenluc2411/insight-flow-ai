CREATE SCHEMA IF NOT EXISTS integration_db;

CREATE TABLE integration_db.connector_configs (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID         NOT NULL,
    connector_type       VARCHAR(50)  NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    status               VARCHAR(30)  NOT NULL DEFAULT 'pending',
    credentials_encrypted TEXT        NOT NULL,
    config               JSONB        NOT NULL DEFAULT '{}'::jsonb,
    webhook_secret       VARCHAR(255),
    last_sync_at         TIMESTAMP WITH TIME ZONE,
    last_error           TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_connector_configs_tenant
    ON integration_db.connector_configs (tenant_id);

CREATE UNIQUE INDEX idx_connector_configs_tenant_type
    ON integration_db.connector_configs (tenant_id, connector_type);
