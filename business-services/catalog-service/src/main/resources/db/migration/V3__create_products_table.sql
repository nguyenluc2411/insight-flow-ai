CREATE TABLE catalog_db.products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    sku_root     VARCHAR(100) NOT NULL,
    name         VARCHAR(500) NOT NULL,
    description  TEXT,
    category_id  UUID REFERENCES catalog_db.categories(id),
    brand        VARCHAR(255),
    season       VARCHAR(50),
    gender       VARCHAR(20),
    tags         TEXT[],
    status       VARCHAR(20) NOT NULL DEFAULT 'active',
    external_ids JSONB DEFAULT '{}'::jsonb,
    raw_data     JSONB,
    source       VARCHAR(50),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku_root)
);

CREATE INDEX idx_products_tenant      ON catalog_db.products(tenant_id);
CREATE INDEX idx_products_category    ON catalog_db.products(category_id);
CREATE INDEX idx_products_external_ids ON catalog_db.products USING GIN(external_ids);
