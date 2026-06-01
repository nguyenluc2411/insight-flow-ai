CREATE SCHEMA IF NOT EXISTS notification_db;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notification_db.notification_aggregation_windows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregation_key VARCHAR(200) NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    aggregated_count INTEGER NOT NULL DEFAULT 0,
    last_notification_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_aggregation_key_window UNIQUE (aggregation_key, window_start, window_end)
);

CREATE INDEX idx_aggregation_key ON notification_db.notification_aggregation_windows (aggregation_key);
CREATE INDEX idx_aggregation_window_start ON notification_db.notification_aggregation_windows (window_start);
CREATE INDEX idx_aggregation_window_end ON notification_db.notification_aggregation_windows (window_end);
CREATE INDEX idx_aggregation_active ON notification_db.notification_aggregation_windows (active);

CREATE TABLE notification_db.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID,
    correlation_id UUID,
    source_service VARCHAR(100),
    notification_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    inbox_status VARCHAR(20) NOT NULL,
    recipient_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,
    aggregation_key VARCHAR(200),
    aggregation_window_id UUID,
    read_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notifications_aggregation_window
        FOREIGN KEY (aggregation_window_id)
        REFERENCES notification_db.notification_aggregation_windows (id)
);

CREATE INDEX idx_notifications_recipient_id ON notification_db.notifications (recipient_id);
CREATE INDEX idx_notifications_status ON notification_db.notifications (status);
CREATE INDEX idx_notifications_inbox_status ON notification_db.notifications (inbox_status);
CREATE INDEX idx_notifications_created_at ON notification_db.notifications (created_at);
CREATE INDEX idx_notifications_event_id ON notification_db.notifications (event_id);
CREATE INDEX idx_notifications_aggregation_key ON notification_db.notifications (aggregation_key);
CREATE INDEX idx_notifications_deleted ON notification_db.notifications (deleted);

CREATE TABLE notification_db.notification_delivery_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    failure_reason TEXT,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_delivery_notification
        FOREIGN KEY (notification_id)
        REFERENCES notification_db.notifications (id)
);

CREATE INDEX idx_delivery_notification_id ON notification_db.notification_delivery_history (notification_id);
CREATE INDEX idx_delivery_status ON notification_db.notification_delivery_history (delivery_status);
CREATE INDEX idx_delivery_channel ON notification_db.notification_delivery_history (channel);
CREATE INDEX idx_delivery_created_at ON notification_db.notification_delivery_history (created_at);

CREATE TABLE notification_db.notification_retry_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    retry_status VARCHAR(20) NOT NULL,
    retry_attempt INTEGER NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL,
    last_failure_reason TEXT,
    last_tried_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_retry_notification
        FOREIGN KEY (notification_id)
        REFERENCES notification_db.notifications (id)
);

CREATE INDEX idx_retry_notification_id ON notification_db.notification_retry_queue (notification_id);
CREATE INDEX idx_retry_status ON notification_db.notification_retry_queue (retry_status);
CREATE INDEX idx_retry_next_retry_at ON notification_db.notification_retry_queue (next_retry_at);
CREATE INDEX idx_retry_channel ON notification_db.notification_retry_queue (channel);

CREATE TABLE notification_db.processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    correlation_id UUID,
    source_service VARCHAR(100),
    payload_hash VARCHAR(128),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processed_event_event_id UNIQUE (event_id)
);

CREATE INDEX idx_processed_event_type ON notification_db.processed_events (event_type);
CREATE INDEX idx_processed_event_processed_at ON notification_db.processed_events (processed_at);
CREATE INDEX idx_processed_event_event_id ON notification_db.processed_events (event_id);

CREATE TABLE notification_db.notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_key VARCHAR(100) NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    html_body TEXT,
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_notification_template_key UNIQUE (template_key)
);

CREATE INDEX idx_template_notification_type ON notification_db.notification_templates (notification_type);
CREATE INDEX idx_template_channel ON notification_db.notification_templates (channel);
CREATE INDEX idx_template_active ON notification_db.notification_templates (active);

CREATE TABLE notification_db.user_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    min_severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    mute_until TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_notification_preference_user_type_channel
        UNIQUE (user_id, notification_type, channel)
);

CREATE INDEX idx_preference_user_id ON notification_db.user_notification_preferences (user_id);
CREATE INDEX idx_preference_enabled ON notification_db.user_notification_preferences (enabled);
CREATE INDEX idx_preference_type ON notification_db.user_notification_preferences (notification_type);

CREATE TABLE notification_db.websocket_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(128) NOT NULL,
    user_id UUID NOT NULL,
    node_id VARCHAR(100),
    client_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    disconnected_at TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_websocket_session_id UNIQUE (session_id)
);

CREATE INDEX idx_websocket_user_id ON notification_db.websocket_sessions (user_id);
CREATE INDEX idx_websocket_active ON notification_db.websocket_sessions (active);
CREATE INDEX idx_websocket_last_heartbeat ON notification_db.websocket_sessions (last_heartbeat_at);
CREATE INDEX idx_websocket_connected_at ON notification_db.websocket_sessions (connected_at);
