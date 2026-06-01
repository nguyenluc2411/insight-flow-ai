CREATE TABLE recommendation_db.recommendation_history (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recommendation_id  UUID NOT NULL REFERENCES recommendation_db.recommendations(id) ON DELETE CASCADE,
    previous_status    VARCHAR(30) NOT NULL,
    new_status         VARCHAR(30) NOT NULL,
    change_reason      TEXT,
    changed_by         VARCHAR(100),
    changed_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recommendation_history_recommendation
    ON recommendation_db.recommendation_history(recommendation_id);
CREATE INDEX idx_recommendation_history_changed_at
    ON recommendation_db.recommendation_history(changed_at DESC);
