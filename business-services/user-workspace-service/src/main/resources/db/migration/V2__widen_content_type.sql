-- =============================================================================
-- V2 — Widen file_metadata.content_type.
-- VARCHAR(50) was too small for real MIME types: the official XLSX type
--   application/vnd.openxmlformats-officedocument.spreadsheetml.sheet  (65 chars)
-- overflowed the column, surfacing as a misleading 409 "duplicate-resource"
-- (the global handler maps every DataIntegrityViolationException to 409).
-- =============================================================================
SET search_path TO workspace_db;

ALTER TABLE file_metadata ALTER COLUMN content_type TYPE VARCHAR(150);
