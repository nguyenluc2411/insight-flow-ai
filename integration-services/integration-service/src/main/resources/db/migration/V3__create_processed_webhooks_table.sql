CREATE TABLE integration_db.processed_webhooks (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID,
    connector_type      VARCHAR(50)  NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    external_event_id   VARCHAR(255) NOT NULL,
    payload             TEXT         NOT NULL,
    signature           VARCHAR(255),
    status              VARCHAR(20)  NOT NULL DEFAULT 'pending',
    retry_count         INTEGER      NOT NULL DEFAULT 0,
    processed_at        TIMESTAMP WITH TIME ZONE,
    error               TEXT,
    received_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (connector_type, external_event_id)
);

CREATE INDEX idx_webhooks_pending
    ON integration_db.processed_webhooks (received_at)
    WHERE status = 'pending';
