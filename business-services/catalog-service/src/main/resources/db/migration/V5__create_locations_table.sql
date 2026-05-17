CREATE TABLE catalog_db.locations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(20) NOT NULL,
    address      TEXT,
    city         VARCHAR(100),
    is_active    BOOLEAN DEFAULT TRUE,
    external_ids JSONB DEFAULT '{}'::jsonb,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_locations_tenant ON catalog_db.locations(tenant_id);
