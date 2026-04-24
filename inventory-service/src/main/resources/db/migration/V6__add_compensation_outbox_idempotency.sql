DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_comp_outbox_source_event_sku'
    ) THEN
        ALTER TABLE compensation_outbox
            ADD CONSTRAINT uq_comp_outbox_source_event_sku UNIQUE (source_event_id, sku);
    END IF;
END $$;
