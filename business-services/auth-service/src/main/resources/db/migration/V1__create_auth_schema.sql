CREATE SCHEMA IF NOT EXISTS auth_db;

-- Shared trigger function: sets updated_at = NOW() on every UPDATE.
-- Used by all tables that have an updated_at column.
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
