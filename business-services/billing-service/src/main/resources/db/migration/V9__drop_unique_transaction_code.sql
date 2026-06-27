-- =============================================================================
-- V9 — Cho phép nhiều giao dịch ngân hàng cùng một transaction_code (ca double-pay).
--
-- Bối cảnh: 1 lần checkout sinh 1 mã IFLOWxxxxxx. Nếu khách chuyển khoản 2 lần cho
-- cùng mã đó (vd: 2 điện thoại quét cùng 1 QR), SePay bắn 2 webhook khác sepay_id
-- nhưng TRÙNG transaction_code. Ràng buộc UNIQUE(transaction_code) ở V7 khiến bản
-- ghi thứ hai (giao dịch trùng -> PENDING_REFUND) vi phạm khoá và bị rollback,
-- nên giao dịch lỗi "biến mất" — admin không thấy để hoàn tiền.
--
-- Idempotency vẫn được đảm bảo bởi uq_payment_transactions_sepay_id (mỗi giao dịch
-- ngân hàng 1 dòng). Bỏ UNIQUE trên transaction_code, thay bằng index thường để
-- vẫn tra cứu nhanh khi khôi phục tenant_id/package_code từ giao dịch SUCCESS cũ.
-- =============================================================================
SET search_path TO billing_db;

ALTER TABLE payment_transactions DROP CONSTRAINT IF EXISTS uq_payment_transactions_code;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_code
    ON payment_transactions(transaction_code);
