ALTER TABLE processed_notifications
    ALTER COLUMN event_type SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_notification_inbox_status'
    ) THEN
        ALTER TABLE inbox_event DROP CONSTRAINT chk_notification_inbox_status;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_notification_inbox_status'
    ) THEN
        ALTER TABLE inbox_event
            ADD CONSTRAINT chk_notification_inbox_status
            CHECK (status IN ('RECEIVED', 'IN_PROGRESS', 'PROCESSED'));
    END IF;
END $$;
