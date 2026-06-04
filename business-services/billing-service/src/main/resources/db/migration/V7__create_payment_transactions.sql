-- =============================================================================
-- V7 — SePay payment transactions.
-- Records each bank-transfer payment matched against a checkout code (IFLOWxxxxxx).
-- sepay_id is the SePay transaction id, used for webhook idempotency (one row per
-- bank transaction). Failed/unmatched transfers are kept with status PENDING_REFUND
-- so an admin can reconcile and refund them manually.
-- =============================================================================
SET search_path TO billing_db;

CREATE TABLE payment_transactions (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sepay_id               VARCHAR(100) NOT NULL,
    tenant_id              UUID,                       -- null until a valid order code is matched
    package_code           VARCHAR(50),
    amount                 INTEGER,
    account_number         VARCHAR(50),
    sender_account_number  VARCHAR(50),
    content                TEXT,
    status                 VARCHAR(30),                -- SUCCESS, PENDING_REFUND, REFUNDED
    error_reason           TEXT,
    transaction_code       VARCHAR(50),                -- IFLOWxxxxxx; null for unmatched transfers
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_payment_transactions_sepay_id UNIQUE (sepay_id),
    CONSTRAINT uq_payment_transactions_code     UNIQUE (transaction_code)
);

-- Admin reconciliation lists transactions by status (e.g. PENDING_REFUND), newest first.
CREATE INDEX idx_payment_transactions_status  ON payment_transactions(status);
-- Tenant-scoped lookups for a tenant's payment history.
CREATE INDEX idx_payment_transactions_tenant  ON payment_transactions(tenant_id);
