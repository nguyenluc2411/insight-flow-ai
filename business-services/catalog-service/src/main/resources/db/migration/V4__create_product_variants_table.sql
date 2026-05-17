CREATE TABLE catalog_db.product_variants (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    product_id       UUID NOT NULL REFERENCES catalog_db.products(id) ON DELETE CASCADE,
    sku              VARCHAR(150) NOT NULL,
    barcode          VARCHAR(100),
    size             VARCHAR(20),
    color            VARCHAR(50),
    color_hex        VARCHAR(7),
    cost_price       DECIMAL(15,2),
    selling_price    DECIMAL(15,2) NOT NULL,
    compare_at_price DECIMAL(15,2),
    status           VARCHAR(20) NOT NULL DEFAULT 'active',
    external_ids     JSONB DEFAULT '{}'::jsonb,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_variants_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_variants_tenant  ON catalog_db.product_variants(tenant_id);
CREATE INDEX idx_variants_product ON catalog_db.product_variants(product_id);
