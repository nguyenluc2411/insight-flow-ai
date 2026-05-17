-- Materialized view for ML service to query daily sales aggregates.
-- Refresh: REFRESH MATERIALIZED VIEW CONCURRENTLY sales_db.daily_sales_summary;
CREATE MATERIALIZED VIEW sales_db.daily_sales_summary AS
SELECT
    soi.tenant_id,
    so.location_id,
    soi.variant_id,
    DATE(so.ordered_at)         AS sale_date,
    SUM(soi.quantity)           AS units_sold,
    SUM(soi.line_total)         AS revenue,
    COUNT(DISTINCT so.id)       AS order_count,
    AVG(soi.unit_price)         AS avg_price
FROM sales_db.sales_order_items soi
JOIN sales_db.sales_orders so ON so.id = soi.order_id
WHERE so.status = 'completed'
GROUP BY soi.tenant_id, so.location_id, soi.variant_id, DATE(so.ordered_at);

CREATE UNIQUE INDEX ON sales_db.daily_sales_summary(tenant_id, location_id, variant_id, sale_date);
