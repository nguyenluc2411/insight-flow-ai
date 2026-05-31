CREATE TABLE sales_db.customers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    external_id   VARCHAR(100),
    phone         VARCHAR(20),
    email         VARCHAR(255),
    full_name     VARCHAR(255),
    gender        VARCHAR(20),
    birth_date    DATE,
    rfm_segment   VARCHAR(50),
    total_spent   DECIMAL(15,2) DEFAULT 0,
    order_count   INT DEFAULT 0,
    last_order_at TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customers_tenant_phone UNIQUE (tenant_id, phone)
);

CREATE INDEX idx_customers_tenant ON sales_db.customers(tenant_id);
CREATE INDEX idx_customers_rfm    ON sales_db.customers(tenant_id, rfm_segment);
