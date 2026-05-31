-- Roles: tenant_id is nullable for system-wide default roles.
-- Custom per-tenant roles have tenant_id set.
CREATE TABLE auth_db.roles (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         REFERENCES auth_db.tenants (id) ON DELETE CASCADE,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON auth_db.roles (tenant_id);

CREATE TABLE auth_db.permissions (
    id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    resource VARCHAR(100) NOT NULL,
    action   VARCHAR(50)  NOT NULL,

    CONSTRAINT uq_permissions_resource_action UNIQUE (resource, action)
);

CREATE TABLE auth_db.role_permissions (
    role_id       UUID NOT NULL REFERENCES auth_db.roles (id)       ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES auth_db.permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE auth_db.user_roles (
    user_id UUID NOT NULL REFERENCES auth_db.users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES auth_db.roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON auth_db.user_roles (user_id);

-- ─── Seed: system-wide default roles (tenant_id = NULL) ──────────────────────
INSERT INTO auth_db.roles (name, description) VALUES
    ('OWNER',       'Full access to all tenant resources'),
    ('MANAGER',     'Manage products, inventory, and reports'),
    ('STAFF',       'View and update inventory and sales'),
    ('ACCOUNTANT',  'Access to financial reports and transactions'),
    ('VIEWER',      'Read-only access across all modules');

-- ─── Seed: core permissions ──────────────────────────────────────────────────
INSERT INTO auth_db.permissions (resource, action) VALUES
    ('catalog',    'read'),
    ('catalog',    'write'),
    ('inventory',  'read'),
    ('inventory',  'write'),
    ('sales',      'read'),
    ('sales',      'write'),
    ('reports',    'read'),
    ('users',      'read'),
    ('users',      'write'),
    ('settings',   'read'),
    ('settings',   'write'),
    ('forecasts',  'read'),
    ('integrations', 'read'),
    ('integrations', 'write');

-- ─── Seed: default role → permission mappings ─────────────────────────────
-- OWNER gets everything
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM auth_db.roles r
 CROSS JOIN auth_db.permissions p
 WHERE r.name = 'OWNER' AND r.tenant_id IS NULL;

-- MANAGER: all except users:write, settings:write
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM auth_db.roles r
  JOIN auth_db.permissions p ON TRUE
 WHERE r.name = 'MANAGER' AND r.tenant_id IS NULL
   AND NOT (p.resource = 'users' AND p.action = 'write')
   AND NOT (p.resource = 'settings' AND p.action = 'write');

-- STAFF: catalog/inventory/sales read+write, reports read
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM auth_db.roles r
  JOIN auth_db.permissions p ON TRUE
 WHERE r.name = 'STAFF' AND r.tenant_id IS NULL
   AND p.resource IN ('catalog', 'inventory', 'sales', 'forecasts')
   AND p.action IN ('read', 'write')
    OR (r.name = 'STAFF' AND r.tenant_id IS NULL AND p.resource = 'reports' AND p.action = 'read');

-- ACCOUNTANT: financial reports read, sales read
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM auth_db.roles r
  JOIN auth_db.permissions p ON TRUE
 WHERE r.name = 'ACCOUNTANT' AND r.tenant_id IS NULL
   AND ((p.resource = 'reports'   AND p.action = 'read')
     OR (p.resource = 'sales'     AND p.action = 'read'));

-- VIEWER: all resources, read only
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM auth_db.roles r
  JOIN auth_db.permissions p ON TRUE
 WHERE r.name = 'VIEWER' AND r.tenant_id IS NULL
   AND p.action = 'read';
