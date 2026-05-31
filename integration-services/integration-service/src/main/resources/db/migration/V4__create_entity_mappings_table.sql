CREATE TABLE integration_db.entity_mappings (
    id                   BIGSERIAL    PRIMARY KEY,
    tenant_id            UUID         NOT NULL,
    connector_config_id  UUID         NOT NULL,
    entity_type          VARCHAR(50)  NOT NULL,
    external_id          VARCHAR(255) NOT NULL,
    internal_id          UUID         NOT NULL,
    sync_hash            VARCHAR(64),
    last_synced_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, connector_config_id, entity_type, external_id)
);

CREATE INDEX idx_entity_mappings_internal
    ON integration_db.entity_mappings (tenant_id, entity_type, internal_id);
