CREATE TABLE auth_db.tenants (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    plan        VARCHAR(50)  NOT NULL DEFAULT 'trial',
    status      VARCHAR(20)  NOT NULL DEFAULT 'active',
    trial_ends_at TIMESTAMP,
    settings    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT chk_tenants_plan   CHECK (plan   IN ('trial', 'starter', 'pro')),
    CONSTRAINT chk_tenants_status CHECK (status IN ('active', 'suspended', 'cancelled'))
);

CREATE INDEX idx_tenants_slug ON auth_db.tenants (slug);

CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON auth_db.tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
