CREATE TABLE IF NOT EXISTS analytics_events (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    tenant_id UUID, -- Nullable because visitor might not be a tenant yet
    event_type VARCHAR(50) NOT NULL, -- PAGE_VIEW, SIGNUP_CLICK, REGISTER_SUCCESS, UPGRADE_SUCCESS
    url VARCHAR(1024),
    utm_source VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_events_session_id ON analytics_events(session_id);
CREATE INDEX idx_analytics_events_event_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_events_created_at ON analytics_events(created_at);
