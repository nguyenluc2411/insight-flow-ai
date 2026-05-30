-- V5: Create refresh_tokens table
-- Stores SHA-256 hashes of refresh tokens (never plaintext).
-- revoked_at IS NULL means the token is still potentially valid.

CREATE TABLE auth_db.refresh_tokens
(
    id                 UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id            UUID         NOT NULL,
    tenant_id          UUID         NOT NULL,
    token_hash         VARCHAR(64)  NOT NULL,   -- SHA-256 hex = 64 chars
    device_fingerprint VARCHAR(500),
    expires_at         TIMESTAMPTZ  NOT NULL,
    revoked_at         TIMESTAMPTZ,             -- NULL = not yet revoked
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user   FOREIGN KEY (user_id)   REFERENCES auth_db.users   (id),
    CONSTRAINT fk_rt_tenant FOREIGN KEY (tenant_id) REFERENCES auth_db.tenants (id)
);

CREATE INDEX idx_refresh_tokens_user_id    ON auth_db.refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON auth_db.refresh_tokens (token_hash);
