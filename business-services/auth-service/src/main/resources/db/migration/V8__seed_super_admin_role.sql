-- ─── Seed: platform-wide SUPER_ADMIN role (tenant_id = NULL) ─────────────────
-- The super-admin USER is seeded at startup by SuperAdminSeeder (credentials
-- read from env: ADMIN_EMAIL / ADMIN_PASSWORD), so NO secret lives in this file.
INSERT INTO auth_db.roles (name, description) VALUES
    ('SUPER_ADMIN', 'Platform super administrator — manages all tenants & billing');

-- SUPER_ADMIN inherits every permission (behaves like OWNER across the platform).
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM auth_db.roles r
 CROSS JOIN auth_db.permissions p
 WHERE r.name = 'SUPER_ADMIN' AND r.tenant_id IS NULL;
