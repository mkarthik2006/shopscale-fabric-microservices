DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_outbox_status'
    ) THEN
        ALTER TABLE outbox_event DROP CONSTRAINT chk_outbox_status;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_outbox_status'
    ) THEN
        ALTER TABLE outbox_event
            ADD CONSTRAINT chk_outbox_status
            CHECK (status IN ('PENDING', 'IN_PROGRESS', 'SENT'));
    END IF;
END $$;
