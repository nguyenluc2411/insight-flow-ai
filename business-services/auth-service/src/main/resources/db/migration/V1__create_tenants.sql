-- V1: Create auth_db schema and tenants table
-- Flyway runs this in the auth_db schema context (create-schemas: true in config)

CREATE SCHEMA IF NOT EXISTS auth_db;

CREATE TABLE auth_db.tenants
(
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(50)  NOT NULL,
    plan       VARCHAR(20)  NOT NULL DEFAULT 'trial',   -- trial | starter | pro
    status     VARCHAR(20)  NOT NULL DEFAULT 'active',  -- active | suspended | deleted
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT chk_tenants_plan   CHECK (plan   IN ('trial', 'starter', 'pro')),
    CONSTRAINT chk_tenants_status CHECK (status IN ('active', 'suspended', 'deleted'))
);

CREATE INDEX idx_tenants_slug   ON auth_db.tenants (slug);
CREATE INDEX idx_tenants_status ON auth_db.tenants (status);
