SET search_path TO billing_db;

CREATE TABLE outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    published_at TIMESTAMP,
    published BOOLEAN DEFAULT false
);

CREATE INDEX idx_outbox_published ON outbox(published) WHERE published = false;
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
