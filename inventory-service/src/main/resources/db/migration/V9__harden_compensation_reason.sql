DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_comp_outbox_reason_not_blank'
    ) THEN
        ALTER TABLE compensation_outbox
            ADD CONSTRAINT chk_comp_outbox_reason_not_blank
            CHECK (length(btrim(reason)) > 0);
    END IF;
END $$;
