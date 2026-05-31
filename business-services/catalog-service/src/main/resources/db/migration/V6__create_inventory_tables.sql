-- Current stock snapshot per variant × location
CREATE TABLE catalog_db.inventory_levels (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    variant_id        UUID NOT NULL REFERENCES catalog_db.product_variants(id),
    location_id       UUID NOT NULL REFERENCES catalog_db.locations(id),
    quantity_on_hand  INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    reorder_point     INT,
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_variant_location UNIQUE (variant_id, location_id)
);

CREATE INDEX idx_inventory_tenant  ON catalog_db.inventory_levels(tenant_id);
CREATE INDEX idx_inventory_variant ON catalog_db.inventory_levels(variant_id);

-- Append-only movement log (event sourcing — NO UPDATE/DELETE on this table)
CREATE TABLE catalog_db.inventory_movements (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      UUID NOT NULL,
    variant_id     UUID NOT NULL,
    location_id    UUID NOT NULL,
    movement_type  VARCHAR(30) NOT NULL,
    quantity_change INT NOT NULL,
    reference_type VARCHAR(50),
    reference_id   UUID,
    notes          TEXT,
    created_by     UUID,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_movements_tenant_time
    ON catalog_db.inventory_movements(tenant_id, created_at DESC);
CREATE INDEX idx_movements_variant
    ON catalog_db.inventory_movements(variant_id);
