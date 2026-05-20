CREATE TABLE recommendation_db.recommendation_rules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code            VARCHAR(100) NOT NULL,
    rule_name            VARCHAR(255) NOT NULL,
    recommendation_type  VARCHAR(30) NOT NULL,
    rule_condition       JSONB NOT NULL,
    priority             INTEGER NOT NULL DEFAULT 1,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_recommendation_rules_code UNIQUE (rule_code)
);

CREATE INDEX idx_recommendation_rules_active
    ON recommendation_db.recommendation_rules(is_active);

CREATE INDEX idx_recommendation_rules_type
    ON recommendation_db.recommendation_rules(recommendation_type);
