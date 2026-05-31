-- Idempotency table: prevents duplicate notifications from Kafka redelivery.
-- PK on event_id guarantees at-most-once processing per event.
CREATE TABLE notification_db.processed_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    topic        VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
