CREATE TABLE recommendation_db.recommendation_metrics (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date            DATE NOT NULL,
    recommendation_type    VARCHAR(30) NOT NULL,
    generated_count        INTEGER NOT NULL DEFAULT 0,
    applied_count          INTEGER NOT NULL DEFAULT 0,
    failed_count           INTEGER NOT NULL DEFAULT 0,
    avg_confidence_score   NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_recommendation_metrics_date_type UNIQUE (metric_date, recommendation_type)
);

CREATE INDEX idx_recommendation_metrics_date
    ON recommendation_db.recommendation_metrics(metric_date DESC);
