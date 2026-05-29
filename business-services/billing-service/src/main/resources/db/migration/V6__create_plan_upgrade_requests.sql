-- =============================================================================
-- V6 — Manual upgrade flow (no payment gateway in MVP).
-- User submits an upgrade request; an admin/ops actor approves it (service JWT)
-- which switches the tenant's plan. Records the lifecycle of each request.
-- =============================================================================
SET search_path TO billing_db;

CREATE TABLE plan_upgrade_requests (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID         NOT NULL,
    requested_package_code VARCHAR(50)  NOT NULL,
    billing_cycle          VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    note                   VARCHAR(500),
    resolved_at            TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_upgrade_requests_status ON plan_upgrade_requests(status);
CREATE INDEX idx_upgrade_requests_tenant ON plan_upgrade_requests(tenant_id);
