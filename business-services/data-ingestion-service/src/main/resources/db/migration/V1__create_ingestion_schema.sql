-- =============================================================================
-- V1 — Normalized inventory parsed from uploaded files.
-- products / product_variants / inventory_facts hold the ingested data;
-- ingestion_jobs tracks each per-workspace ingestion run.
-- Every table carries tenant_id; product_code and sku are unique PER TENANT.
-- =============================================================================
SET search_path TO ingestion_db;

CREATE TABLE products (
    id                 VARCHAR(36)  PRIMARY KEY,
    tenant_id          VARCHAR(36)  NOT NULL,
    product_code       VARCHAR(50)  NOT NULL,
    product_name       VARCHAR(255) NOT NULL,
    brand              VARCHAR(100),
    department         VARCHAR(50),
    category           VARCHAR(50),
    sub_category       VARCHAR(50),
    target_demographic VARCHAR(50),
    material           VARCHAR(100),
    fit_type           VARCHAR(50),
    pattern            VARCHAR(50),
    style_context      VARCHAR(50),
    season             VARCHAR(50),
    attributes         JSONB,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_products_tenant_code UNIQUE (tenant_id, product_code)
);
CREATE INDEX idx_products_tenant ON products(tenant_id);

CREATE TABLE product_variants (
    id           VARCHAR(36)  PRIMARY KEY,
    tenant_id    VARCHAR(36)  NOT NULL,
    product_id   VARCHAR(36)  NOT NULL,
    sku          VARCHAR(100) NOT NULL,
    color_family VARCHAR(50),
    color_name   VARCHAR(50),
    size         VARCHAR(20),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_variants_tenant_sku UNIQUE (tenant_id, sku),
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
);
CREATE INDEX idx_variants_tenant  ON product_variants(tenant_id);
CREATE INDEX idx_variants_product ON product_variants(product_id);

CREATE TABLE inventory_facts (
    id                 VARCHAR(36)  PRIMARY KEY,
    tenant_id          VARCHAR(36)  NOT NULL,
    variant_id         VARCHAR(36)  NOT NULL,
    workspace_id       VARCHAR(36)  NOT NULL,
    warehouse_location VARCHAR(100),
    cost_price         DOUBLE PRECISION,
    retail_price       DOUBLE PRECISION,
    current_price      DOUBLE PRECISION,
    currency           VARCHAR(10),
    quantity_in_stock  INTEGER      NOT NULL,
    quantity_sold      INTEGER,
    import_date        DATE,
    last_sold_date     DATE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_facts_variant_workspace UNIQUE (variant_id, workspace_id),
    CONSTRAINT fk_facts_variant FOREIGN KEY (variant_id)
        REFERENCES product_variants(id) ON DELETE CASCADE
);
CREATE INDEX idx_facts_tenant    ON inventory_facts(tenant_id);
CREATE INDEX idx_facts_workspace ON inventory_facts(workspace_id);

CREATE TABLE ingestion_jobs (
    id                VARCHAR(36) PRIMARY KEY,
    tenant_id         VARCHAR(36) NOT NULL,
    workspace_id      VARCHAR(36) NOT NULL,
    status            VARCHAR(20) NOT NULL,    -- PROCESSING, DONE, ERROR
    total_records     INTEGER     NOT NULL,
    processed_records INTEGER     NOT NULL,
    failed_records    INTEGER     NOT NULL,
    error_log         JSONB,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ingestion_jobs_workspace UNIQUE (workspace_id)
);
CREATE INDEX idx_ingestion_jobs_tenant ON ingestion_jobs(tenant_id);
