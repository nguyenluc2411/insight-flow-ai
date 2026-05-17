CREATE TABLE sales_db.suppliers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    name         VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    phone        VARCHAR(20),
    email        VARCHAR(255),
    address      TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_suppliers_tenant ON sales_db.suppliers(tenant_id);
