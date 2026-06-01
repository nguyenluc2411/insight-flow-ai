CREATE TABLE recommendation_db.recommendations (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id             UUID NOT NULL,
    warehouse_id           UUID,
    recommendation_type    VARCHAR(30) NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    priority               VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    confidence_score       NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (confidence_score >= 0),
    risk_level             VARCHAR(20) NOT NULL DEFAULT 'LOW',
    recommendation_reason  TEXT,
    action_payload         JSONB NOT NULL DEFAULT '{}'::jsonb,
    event_id               UUID NOT NULL,
    generated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at           TIMESTAMP WITH TIME ZONE,
    expires_at             TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version                INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uq_recommendations_event_id UNIQUE (event_id)
);

CREATE INDEX idx_recommendations_product
    ON recommendation_db.recommendations(product_id);
CREATE INDEX idx_recommendations_status
    ON recommendation_db.recommendations(status);
CREATE INDEX idx_recommendations_generated_at
    ON recommendation_db.recommendations(generated_at DESC);
CREATE INDEX idx_recommendations_type_status
    ON recommendation_db.recommendations(recommendation_type, status);
CREATE INDEX idx_recommendations_warehouse
    ON recommendation_db.recommendations(warehouse_id);
