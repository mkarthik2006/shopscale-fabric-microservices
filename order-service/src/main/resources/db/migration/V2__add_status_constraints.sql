DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_orders_status'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_status
            CHECK (status IN ('PLACED', 'CANCELLED'));
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
            CHECK (status IN ('PENDING', 'SENT'));
    END IF;
END $$;
