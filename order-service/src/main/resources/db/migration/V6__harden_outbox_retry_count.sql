DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_outbox_retry_count_non_negative'
    ) THEN
        ALTER TABLE outbox_event
            ADD CONSTRAINT chk_outbox_retry_count_non_negative
            CHECK (retry_count >= 0);
    END IF;
END $$;
