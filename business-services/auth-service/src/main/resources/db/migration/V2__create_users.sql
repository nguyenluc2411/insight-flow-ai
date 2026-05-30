-- V2: Create users table
-- Email is unique per tenant (same email can exist in different tenants).
-- Users log in with email + password + tenantSlug.

CREATE TABLE auth_db.users
(
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    status        VARCHAR(20)  NOT NULL DEFAULT 'active',  -- active | inactive | locked
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_users_tenant    FOREIGN KEY (tenant_id) REFERENCES auth_db.tenants (id),
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT chk_users_status   CHECK (status IN ('active', 'inactive', 'locked'))
);

CREATE INDEX idx_users_tenant_id ON auth_db.users (tenant_id);
CREATE INDEX idx_users_email     ON auth_db.users (email);
