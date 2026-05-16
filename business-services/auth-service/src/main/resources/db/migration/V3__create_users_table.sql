CREATE TABLE auth_db.users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES auth_db.tenants (id) ON DELETE CASCADE,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    phone         VARCHAR(20),
    status        VARCHAR(20)  NOT NULL DEFAULT 'active',
    last_login_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT chk_users_status CHECK (status IN ('active', 'inactive', 'locked'))
);

CREATE INDEX idx_users_tenant_id    ON auth_db.users (tenant_id);
CREATE INDEX idx_users_tenant_email ON auth_db.users (tenant_id, email);

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON auth_db.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
