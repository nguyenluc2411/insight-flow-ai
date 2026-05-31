CREATE TABLE auth_db.password_reset_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES auth_db.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_prt_hash UNIQUE (token_hash)
);

CREATE INDEX idx_prt_user_id ON auth_db.password_reset_tokens(user_id);
CREATE INDEX idx_prt_hash    ON auth_db.password_reset_tokens(token_hash);
