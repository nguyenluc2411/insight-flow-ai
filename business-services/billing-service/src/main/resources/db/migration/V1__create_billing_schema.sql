CREATE SCHEMA IF NOT EXISTS billing_db;

SET search_path TO billing_db;

CREATE TABLE packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    display_order INT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(code, version)
);

CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES packages(id),
    billing_cycle VARCHAR(20) NOT NULL,  -- MONTHLY, YEARLY, ONE_TIME
    price_vnd INT NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    trial_days INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(package_id, billing_cycle)
);

CREATE TABLE features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE package_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES packages(id),
    feature_id UUID NOT NULL REFERENCES features(id),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(package_id, feature_id)
);

CREATE TABLE plan_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL UNIQUE REFERENCES packages(id),
    max_api_calls_per_day INT DEFAULT -1,
    max_storage_gb INT DEFAULT -1,
    max_users INT DEFAULT -1,
    api_rate_limit_per_minute INT DEFAULT -1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES plans(id),
    price_at_subscription INT NOT NULL,
    features_at_subscription JSONB NOT NULL DEFAULT '[]',
    limits_at_subscription JSONB NOT NULL DEFAULT '{}',
    plan_version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    auto_renew BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tenant_subscriptions_tenant_id ON tenant_subscriptions(tenant_id);
CREATE INDEX idx_tenant_subscriptions_status ON tenant_subscriptions(status);

CREATE TABLE tenant_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    api_calls_count INT DEFAULT 0,
    product_exports_count INT DEFAULT 0,
    reports_generated_count INT DEFAULT 0,
    forecasts_executed_count INT DEFAULT 0,
    storage_used_bytes BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, usage_date)
);

CREATE INDEX idx_tenant_usage_tenant_id ON tenant_usage(tenant_id);
CREATE INDEX idx_tenant_usage_date ON tenant_usage(usage_date);

CREATE TABLE tenant_user_count (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    user_count INT DEFAULT 0,
    last_updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE billing_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    subscription_id UUID REFERENCES tenant_subscriptions(id),
    event_type VARCHAR(50),
    from_package_code VARCHAR(50),
    to_package_code VARCHAR(50),
    amount_vnd INT,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_billing_history_tenant_id ON billing_history(tenant_id);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    subscription_id UUID REFERENCES tenant_subscriptions(id),
    event_type VARCHAR(50),
    reason VARCHAR(255),
    attempt_count INT DEFAULT 1,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
