-- =============================================================================
-- init.sql — Runs once when PostgreSQL container starts for the first time.
-- Creates one database per service (database-per-service pattern).
-- The default DB (POSTGRES_DB env) still exists; these are additional ones.
-- =============================================================================

CREATE DATABASE insightflow_auth;
CREATE DATABASE insightflow_catalog;
CREATE DATABASE insightflow_sales;
CREATE DATABASE insightflow_integration;
CREATE DATABASE insightflow_notification;
CREATE DATABASE insightflow_ml;
CREATE DATABASE insightflow_billing;
CREATE DATABASE insightflow_workspace;
CREATE DATABASE insightflow_ingestion;
