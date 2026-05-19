CREATE TABLE notification_db.notification_preferences (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL,
    user_id      UUID,
    event_type   VARCHAR(50) NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    enabled      BOOLEAN     NOT NULL DEFAULT true,
    threshold    JSONB,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pref_tenant_user_event_channel
        UNIQUE (tenant_id, user_id, event_type, channel)
);

CREATE INDEX idx_prefs_tenant ON notification_db.notification_preferences (tenant_id);
