-- Add recipient_email column to notifications table
ALTER TABLE notification_db.notifications
ADD COLUMN recipient_email VARCHAR(255);

-- Create index for email lookups
CREATE INDEX idx_notifications_recipient_email ON notification_db.notifications (recipient_email);

-- Add constraint to ensure at least one of recipientId or recipientEmail is present
-- (notification-service can use either for lookup, preferring email when available)
ALTER TABLE notification_db.notifications
ADD CONSTRAINT check_recipient_identifier 
    CHECK (recipient_id IS NOT NULL OR recipient_email IS NOT NULL);
