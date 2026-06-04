-- Email-only login: a user's email is now globally unique (was unique per tenant).
-- This lets users sign in with just email + password — no tenant slug required.
-- Trade-off: one email can no longer own/belong to more than one tenant.
--
-- NOTE: if existing data has the same email under two different tenants, the
-- ADD CONSTRAINT below will fail. Resolve duplicates before applying in that case.

ALTER TABLE auth_db.users DROP CONSTRAINT IF EXISTS uq_users_tenant_email;

ALTER TABLE auth_db.users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- The composite (tenant_id, email) lookup index is now redundant:
-- uq_users_email gives a unique index on email, idx_users_tenant_id covers tenant scans.
DROP INDEX IF EXISTS auth_db.idx_users_tenant_email;
