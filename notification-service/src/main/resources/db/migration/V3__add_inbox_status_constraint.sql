DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_notification_inbox_status'
    ) THEN
        ALTER TABLE inbox_event
            ADD CONSTRAINT chk_notification_inbox_status
            CHECK (status IN ('RECEIVED', 'PROCESSED'));
    END IF;
END $$;
