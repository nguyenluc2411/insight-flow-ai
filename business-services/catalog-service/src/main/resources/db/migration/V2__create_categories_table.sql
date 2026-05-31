CREATE TABLE catalog_db.categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    parent_id   UUID REFERENCES catalog_db.categories(id),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    level       INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE INDEX idx_categories_tenant ON catalog_db.categories(tenant_id);
