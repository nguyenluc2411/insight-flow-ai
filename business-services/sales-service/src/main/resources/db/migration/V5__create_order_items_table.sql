-- Append-only order line items. NO updated_at column intentionally.
CREATE TABLE sales_db.sales_order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    order_id        UUID NOT NULL REFERENCES sales_db.sales_orders(id) ON DELETE CASCADE,
    variant_id      UUID NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(15,2) NOT NULL,
    unit_cost       DECIMAL(15,2),
    discount_amount DECIMAL(15,2) DEFAULT 0,
    line_total      DECIMAL(15,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_items_order   ON sales_db.sales_order_items(order_id);
CREATE INDEX idx_items_variant ON sales_db.sales_order_items(variant_id);
CREATE INDEX idx_items_tenant  ON sales_db.sales_order_items(tenant_id);
