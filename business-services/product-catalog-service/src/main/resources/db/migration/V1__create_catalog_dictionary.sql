-- =============================================================================
-- V1 — Catalog enrichment dictionary.
-- Global reference data (NOT tenant-scoped): maps a raw synonym/typo to a
-- standardized fashion-attribute value, e.g. ("COLOR", "đỏ đô") -> "Red".
-- The service loads all rows into a RAM cache and fuzzy-matches against them.
--
-- NOTE: this table ships EMPTY. Until it is seeded with synonym rows the
-- enrichment endpoint returns only default values (Unknown / Unisex / ...).
-- Seed data is owned by the catalog/fashion taxonomy and added separately.
-- =============================================================================
SET search_path TO enrichment_db;

CREATE TABLE catalog_dictionaries (
    id             VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid(),
    attribute_type VARCHAR(50)  NOT NULL,   -- COLOR, MATERIAL, DEPARTMENT, CATEGORY, SUB_CATEGORY, FIT_TYPE, ...
    standard_value VARCHAR(100) NOT NULL,   -- normalized value sent downstream (e.g. Red, Cotton, Oversize)
    synonym        VARCHAR(100) NOT NULL    -- raw keyword / slang / misspelling (e.g. đỏ đô, coton, ôm body)
);

CREATE INDEX idx_attribute_synonym ON catalog_dictionaries(attribute_type, synonym);
