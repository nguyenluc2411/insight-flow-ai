CREATE TABLE sales_db.sales_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    order_number    VARCHAR(50) NOT NULL,
    location_id     UUID,
    customer_id     UUID REFERENCES sales_db.customers(id),
    channel         VARCHAR(30) NOT NULL DEFAULT 'pos',
    status          VARCHAR(30) NOT NULL DEFAULT 'pending',
    subtotal        DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    discount_type   VARCHAR(30),
    tax_amount      DECIMAL(15,2) DEFAULT 0,
    shipping_amount DECIMAL(15,2) DEFAULT 0,
    total_amount    DECIMAL(15,2) NOT NULL,
    payment_method  VARCHAR(30),
    external_id     VARCHAR(100),
    source          VARCHAR(50),
    ordered_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    raw_data        JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_orders_tenant_number UNIQUE (tenant_id, order_number)
);

CREATE INDEX idx_orders_tenant_time ON sales_db.sales_orders(tenant_id, ordered_at DESC);
CREATE INDEX idx_orders_customer    ON sales_db.sales_orders(customer_id);
CREATE INDEX idx_orders_location    ON sales_db.sales_orders(location_id);
CREATE INDEX idx_orders_status      ON sales_db.sales_orders(tenant_id, status);
