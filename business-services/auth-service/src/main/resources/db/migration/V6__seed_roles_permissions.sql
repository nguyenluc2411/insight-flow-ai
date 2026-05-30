-- V6: Seed default roles and permissions
-- Roles: OWNER > MANAGER > STAFF | ACCOUNTANT | VIEWER
-- Permissions follow resource:action pattern matching service domains.

-- ── Permissions ────────────────────────────────────────────────────────────
INSERT INTO auth_db.permissions (name, description) VALUES
    ('catalog:read',          'View products, variants, and inventory'),
    ('catalog:write',         'Create and update products and variants'),
    ('inventory:read',        'View inventory levels and movements'),
    ('inventory:write',       'Adjust inventory levels'),
    ('sales:read',            'View orders and customers'),
    ('sales:write',           'Create and update orders'),
    ('forecast:read',         'View demand forecasts'),
    ('recommendation:read',   'View inventory recommendations'),
    ('integration:read',      'View POS connector status and sync logs'),
    ('integration:write',     'Configure and trigger POS connectors'),
    ('admin:users',           'Manage users within the tenant'),
    ('admin:settings',        'Manage tenant-level settings');

-- ── Roles ──────────────────────────────────────────────────────────────────
INSERT INTO auth_db.roles (name, description) VALUES
    ('OWNER',      'Full access — tenant owner'),
    ('MANAGER',    'Operational access — manage staff, view all data'),
    ('STAFF',      'Day-to-day sales and inventory operations'),
    ('ACCOUNTANT', 'Read-only access to sales and financial data'),
    ('VIEWER',     'Read-only access to dashboards and reports');

-- ── Role → Permission mappings ─────────────────────────────────────────────

-- OWNER gets everything
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   auth_db.roles r
CROSS JOIN auth_db.permissions p
WHERE  r.name = 'OWNER';

-- MANAGER: all except tenant settings
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   auth_db.roles r
JOIN   auth_db.permissions p ON p.name IN (
           'catalog:read', 'catalog:write',
           'inventory:read', 'inventory:write',
           'sales:read', 'sales:write',
           'forecast:read', 'recommendation:read',
           'integration:read', 'integration:write',
           'admin:users')
WHERE  r.name = 'MANAGER';

-- STAFF: catalog + inventory + sales operations
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   auth_db.roles r
JOIN   auth_db.permissions p ON p.name IN (
           'catalog:read',
           'inventory:read', 'inventory:write',
           'sales:read', 'sales:write',
           'forecast:read', 'recommendation:read')
WHERE  r.name = 'STAFF';

-- ACCOUNTANT: read-only sales + forecasts
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   auth_db.roles r
JOIN   auth_db.permissions p ON p.name IN (
           'catalog:read',
           'inventory:read',
           'sales:read',
           'forecast:read', 'recommendation:read')
WHERE  r.name = 'ACCOUNTANT';

-- VIEWER: read-only everything
INSERT INTO auth_db.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   auth_db.roles r
JOIN   auth_db.permissions p ON p.name IN (
           'catalog:read',
           'inventory:read',
           'sales:read',
           'forecast:read', 'recommendation:read')
WHERE  r.name = 'VIEWER';
