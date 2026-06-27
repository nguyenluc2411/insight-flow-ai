-- =============================================================================
-- V8 — Refund reconciliation enrichment.
--
-- payment_transactions gains the billing-owned fields an admin needs to identify
-- and reconcile a transfer that must be refunded:
--   - reference_code   : SePay/bank reference (mã tham chiếu) — the reliable key to
--                        cross-check against a bank statement. SePay does NOT send
--                        the sender's bank account number, so sender_account_number
--                        is usually null; reference_code is the dependable đối chứng key.
--   - transaction_date : the actual bank-transfer timestamp from the webhook
--                        (created_at is only the DB insert time).
--
-- Customer contact (email/name) is intentionally NOT stored here. It lives in
-- auth-service (insightflow_auth) — the single source of truth. The admin UI
-- resolves it from the transaction's tenant_id via auth-service's existing admin
-- API (GET /api/v1/admin/tenants/{id}), so we avoid duplicating PII across services.
-- =============================================================================
SET search_path TO billing_db;

ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS reference_code   VARCHAR(100);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMP;

-- Admin reconciliation cross-checks a transfer by its bank reference code.
CREATE INDEX IF NOT EXISTS idx_payment_transactions_reference_code
    ON payment_transactions(reference_code);

-- Duplicate-payment detection looks up a recent SUCCESS transaction for the same
-- (tenant, package). Composite index keeps that probe cheap.
CREATE INDEX IF NOT EXISTS idx_payment_transactions_tenant_pkg_status
    ON payment_transactions(tenant_id, package_code, status);
