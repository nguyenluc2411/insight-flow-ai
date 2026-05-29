-- Additional indexes and constraints for billing_db
-- Primary indexes are already created in V1.
-- This migration is reserved for future index additions.

SET search_path TO billing_db;

-- Index on plan_limits for fast lookups by package
CREATE INDEX IF NOT EXISTS idx_plan_limits_package_id ON plan_limits(package_id);

-- Index on package_features for fast entitlement lookups
CREATE INDEX IF NOT EXISTS idx_package_features_package_id ON package_features(package_id);
CREATE INDEX IF NOT EXISTS idx_package_features_feature_id ON package_features(feature_id);

-- Index on payment_events for retry processing
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_id ON payment_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_next_retry ON payment_events(next_retry_at) WHERE next_retry_at IS NOT NULL;
