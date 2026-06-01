ALTER TABLE notification_db.notification_retry_queue
    ADD COLUMN IF NOT EXISTS failure_type VARCHAR(20);

ALTER TABLE notification_db.notification_delivery_history
    ADD COLUMN IF NOT EXISTS failure_type VARCHAR(20);

CREATE TABLE IF NOT EXISTS notification_db.notification_dlq_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID,
    event_id UUID,
    correlation_id UUID,
    recipient_id UUID,
    channel VARCHAR(20),
    event_type VARCHAR(120),
    failure_type VARCHAR(20),
    failure_reason TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    source_service VARCHAR(100),
    payload JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_dlq_notification
        FOREIGN KEY (notification_id)
        REFERENCES notification_db.notifications (id)
);

CREATE INDEX IF NOT EXISTS idx_dlq_notification_id ON notification_db.notification_dlq_records (notification_id);
CREATE INDEX IF NOT EXISTS idx_dlq_event_id ON notification_db.notification_dlq_records (event_id);
CREATE INDEX IF NOT EXISTS idx_dlq_recipient_id ON notification_db.notification_dlq_records (recipient_id);
CREATE INDEX IF NOT EXISTS idx_dlq_created_at ON notification_db.notification_dlq_records (created_at);
