CREATE TABLE IF NOT EXISTS compensation_outbox (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    source_event_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_comp_outbox_status_created_at
    ON compensation_outbox (status, created_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_comp_outbox_status'
    ) THEN
        ALTER TABLE compensation_outbox
            ADD CONSTRAINT chk_comp_outbox_status
            CHECK (status IN ('PENDING', 'SENT'));
    END IF;
END $$;
