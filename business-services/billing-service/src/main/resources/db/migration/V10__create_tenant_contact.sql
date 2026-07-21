-- =============================================================================
-- V10: Bảng tenant_contact — cache email/tên chủ tenant từ sự kiện
--      auth.tenant.registered, phục vụ gửi hóa đơn/biên nhận thanh toán.
-- =============================================================================
CREATE TABLE IF NOT EXISTS billing_db.tenant_contact (
    tenant_id   UUID PRIMARY KEY,
    email       VARCHAR(255),
    name        VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tenant_contact_email
    ON billing_db.tenant_contact (email);
