CREATE TABLE notification_db.notifications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL,
    user_id      UUID,
    type         VARCHAR(50) NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT,
    metadata     JSONB,
    is_read      BOOLEAN     NOT NULL DEFAULT false,
    sent_at      TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_tenant_unread
    ON notification_db.notifications (tenant_id, is_read, created_at DESC);

CREATE INDEX idx_notifications_tenant_type
    ON notification_db.notifications (tenant_id, type);
