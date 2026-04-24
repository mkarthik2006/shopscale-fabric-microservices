ALTER TABLE processed_events
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

ALTER TABLE processed_events
    ALTER COLUMN status SET DEFAULT 'PROCESSED';

UPDATE processed_events
SET status = 'PROCESSED'
WHERE status IS NULL;

ALTER TABLE processed_events
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE processed_events
    ADD CONSTRAINT chk_processed_events_status
    CHECK (status IN ('PROCESSED', 'FAILED', 'RETRYING'));

ALTER TABLE processed_events
    ALTER COLUMN processed_at SET DEFAULT CURRENT_TIMESTAMP;

UPDATE processed_events
SET processed_at = CURRENT_TIMESTAMP
WHERE processed_at IS NULL;

ALTER TABLE processed_events
    ALTER COLUMN processed_at SET NOT NULL;
