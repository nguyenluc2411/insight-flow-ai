CREATE TABLE recommendation_db.recommendation_audit (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recommendation_id   UUID REFERENCES recommendation_db.recommendations(id) ON DELETE SET NULL,
    event_id            UUID NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    action_type         VARCHAR(100) NOT NULL,
    processing_status   VARCHAR(30) NOT NULL,
    error_message       TEXT,
    payload_snapshot    JSONB,
    processed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recommendation_audit_recommendation
    ON recommendation_db.recommendation_audit(recommendation_id);
CREATE INDEX idx_recommendation_audit_event
    ON recommendation_db.recommendation_audit(event_id);
CREATE INDEX idx_recommendation_audit_status
    ON recommendation_db.recommendation_audit(processing_status);
