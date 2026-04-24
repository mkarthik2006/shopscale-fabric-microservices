ALTER TABLE processed_notifications
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

ALTER TABLE processed_notifications
    ALTER COLUMN status SET DEFAULT 'PROCESSED';

UPDATE processed_notifications
SET status = 'PROCESSED'
WHERE status IS NULL;

ALTER TABLE processed_notifications
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE processed_notifications
    ADD CONSTRAINT chk_processed_notifications_status
    CHECK (status IN ('PROCESSED', 'FAILED', 'RETRYING'));

ALTER TABLE processed_notifications
    ALTER COLUMN processed_at SET DEFAULT CURRENT_TIMESTAMP;

UPDATE processed_notifications
SET processed_at = CURRENT_TIMESTAMP
WHERE processed_at IS NULL;

ALTER TABLE processed_notifications
    ALTER COLUMN processed_at SET NOT NULL;
