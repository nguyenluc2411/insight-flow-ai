-- =============================================================================
-- V2 — Cho phép products.product_name NULL.
-- Triết lý ingest: rã được tới đâu lưu tới đó; trường thiếu thì để NULL thay vì
-- chặn cả dòng. Trước đây product_name NOT NULL khiến 1 dòng thiếu tên ném lỗi
-- constraint -> abort cả transaction batch (PSQLException 25P02), giết cả file.
-- =============================================================================
SET search_path TO ingestion_db;

ALTER TABLE products ALTER COLUMN product_name DROP NOT NULL;
