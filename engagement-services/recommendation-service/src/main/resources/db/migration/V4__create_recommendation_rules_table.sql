CREATE TABLE recommendation_db.recommendation_rules (
    ON recommendation_db.recommendation_rules(is_active);
CREATE INDEX idx_recommendation_rules_active
    ON recommendation_db.recommendation_rules(recommendation_type);
CREATE INDEX idx_recommendation_rules_type

);
    CONSTRAINT uq_recommendation_rules_code UNIQUE (rule_code)
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    priority             INTEGER NOT NULL DEFAULT 1,
    rule_condition       JSONB NOT NULL,
    recommendation_type  VARCHAR(30) NOT NULL,
    rule_name            VARCHAR(255) NOT NULL,
    rule_code            VARCHAR(100) NOT NULL,
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
