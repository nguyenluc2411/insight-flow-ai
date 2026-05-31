-- =============================================================================
-- V5 — Restructure packages for MVP launch.
-- Final tier ladder: FREE, TRIAL(=Advanced, 30d), BASIC, ADVANCED, PRO.
-- STARTER is deprecated (kept, never deleted — existing snapshots stay valid).
-- Feature codes are seeded now so future @RequireFeature / EntitlementInterceptor
-- (enforcement plan B) can be wired without another data migration.
-- =============================================================================
SET search_path TO billing_db;

-- ── New feature: AI Advisor (cold-start suggestions) ─────────────────────────
INSERT INTO features (id, code, name, category) VALUES
  ('c0000001-0000-0000-0000-000000000010', 'AI_ADVISOR', 'AI Advisor (cold-start)', 'AI')
ON CONFLICT (code) DO NOTHING;

-- ── New packages: FREE, BASIC, ADVANCED ──────────────────────────────────────
INSERT INTO packages (id, code, version, name, description, display_order, status) VALUES
  ('a0000001-0000-0000-0000-000000000004', 'FREE',     1, 'Free',     'Mien phi - giu user trong he sinh thai', 0, 'ACTIVE'),
  ('a0000001-0000-0000-0000-000000000005', 'BASIC',    1, 'Basic',    'Shop moi mo / shop nho',                  2, 'ACTIVE'),
  ('a0000001-0000-0000-0000-000000000006', 'ADVANCED', 1, 'Advanced', 'Shop dang tang truong, co du lieu',       3, 'ACTIVE')
ON CONFLICT (code, version) DO NOTHING;

-- TRIAL display order + keep ACTIVE (Trial experience = Advanced level)
UPDATE packages SET display_order = 1, description = 'Dung thu 30 ngay - mo muc Advanced'
  WHERE code = 'TRIAL';
UPDATE packages SET display_order = 4, description = 'Chuoi cua hang / doanh nghiep'
  WHERE code = 'PRO';

-- Deprecate STARTER (do NOT delete — old subscriptions snapshot still reference it)
UPDATE packages SET status = 'DEPRECATED' WHERE code = 'STARTER';

-- ── Plans (pricing rows) ─────────────────────────────────────────────────────
INSERT INTO plans (id, package_id, billing_cycle, price_vnd, trial_days, status) VALUES
  ('b0000001-0000-0000-0000-000000000006', 'a0000001-0000-0000-0000-000000000004', 'MONTHLY',     0, 0, 'ACTIVE'),
  ('b0000001-0000-0000-0000-000000000007', 'a0000001-0000-0000-0000-000000000005', 'MONTHLY', 199000, 0, 'ACTIVE'),
  ('b0000001-0000-0000-0000-000000000008', 'a0000001-0000-0000-0000-000000000006', 'MONTHLY', 499000, 0, 'ACTIVE')
ON CONFLICT (package_id, billing_cycle) DO NOTHING;

-- TRIAL: 30-day trial period
UPDATE plans SET trial_days = 30 WHERE package_id = 'a0000001-0000-0000-0000-000000000001';

-- PRO monthly: 799k -> 699k for launch
UPDATE plans SET price_vnd = 699000
  WHERE package_id = 'a0000001-0000-0000-0000-000000000003' AND billing_cycle = 'MONTHLY';

-- Deprecate STARTER plans
UPDATE plans SET status = 'DEPRECATED' WHERE package_id = 'a0000001-0000-0000-0000-000000000002';

-- ── Plan limits ──────────────────────────────────────────────────────────────
-- FREE: 1 user, 500 api/day, 1GB, 30 rpm
INSERT INTO plan_limits (package_id, max_api_calls_per_day, max_storage_gb, max_users, api_rate_limit_per_minute) VALUES
  ('a0000001-0000-0000-0000-000000000004',  500,   1,  1,  30),
  ('a0000001-0000-0000-0000-000000000005', 1000,   5,  3,  30),
  ('a0000001-0000-0000-0000-000000000006', 5000,  20, 10,  60)
ON CONFLICT (package_id) DO NOTHING;

-- TRIAL limits = Advanced level (Trial unlocks Advanced)
UPDATE plan_limits SET max_api_calls_per_day = 5000, max_storage_gb = 20, max_users = 10, api_rate_limit_per_minute = 60
  WHERE package_id = 'a0000001-0000-0000-0000-000000000001';

-- PRO limits: high but bounded (no longer unlimited) — 50 users, 50k api/day, 100GB, 300 rpm
UPDATE plan_limits SET max_api_calls_per_day = 50000, max_storage_gb = 100, max_users = 50, api_rate_limit_per_minute = 300
  WHERE package_id = 'a0000001-0000-0000-0000-000000000003';

-- ── Package ↔ feature mapping ────────────────────────────────────────────────
-- FREE: just 1 POS connection (KiotViet). No premium features.
INSERT INTO package_features (package_id, feature_id)
SELECT 'a0000001-0000-0000-0000-000000000004', id FROM features WHERE code IN
  ('KIOTVIET_INTEGRATION')
ON CONFLICT (package_id, feature_id) DO NOTHING;

-- BASIC: Free + AI Advisor + Sales Analytics
INSERT INTO package_features (package_id, feature_id)
SELECT 'a0000001-0000-0000-0000-000000000005', id FROM features WHERE code IN
  ('KIOTVIET_INTEGRATION', 'AI_ADVISOR', 'SALES_ANALYTICS')
ON CONFLICT (package_id, feature_id) DO NOTHING;

-- ADVANCED: Basic + Forecast + Recommend + Multi-location + Export
INSERT INTO package_features (package_id, feature_id)
SELECT 'a0000001-0000-0000-0000-000000000006', id FROM features WHERE code IN
  ('KIOTVIET_INTEGRATION', 'AI_ADVISOR', 'SALES_ANALYTICS',
   'DEMAND_FORECAST', 'INVENTORY_RECOMMEND', 'MULTI_LOCATION', 'EXPORT_REPORTS')
ON CONFLICT (package_id, feature_id) DO NOTHING;

-- TRIAL: same feature set as Advanced
INSERT INTO package_features (package_id, feature_id)
SELECT 'a0000001-0000-0000-0000-000000000001', id FROM features WHERE code IN
  ('KIOTVIET_INTEGRATION', 'AI_ADVISOR', 'SALES_ANALYTICS',
   'DEMAND_FORECAST', 'INVENTORY_RECOMMEND', 'MULTI_LOCATION', 'EXPORT_REPORTS')
ON CONFLICT (package_id, feature_id) DO NOTHING;

-- PRO: everything (top tier) — ensure all features incl. the new AI_ADVISOR
INSERT INTO package_features (package_id, feature_id)
SELECT 'a0000001-0000-0000-0000-000000000003', id FROM features
ON CONFLICT (package_id, feature_id) DO NOTHING;
