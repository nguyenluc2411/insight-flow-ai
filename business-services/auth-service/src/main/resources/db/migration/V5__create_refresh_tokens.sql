CREATE TABLE auth_db.refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES auth_db.users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked_at  TIMESTAMP,
    device_info JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

-- Partial index — only active (non-revoked) tokens are hot-path lookups.
CREATE INDEX idx_refresh_tokens_user_active
    ON auth_db.refresh_tokens (user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_refresh_tokens_hash
    ON auth_db.refresh_tokens (token_hash);
