ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(254);

UPDATE orders
SET user_email = CASE
    WHEN user_id LIKE '%@%' THEN user_id
    ELSE user_id || '@shopscale.dev'
END
WHERE user_email IS NULL;

ALTER TABLE orders
    ALTER COLUMN user_email SET NOT NULL;

ALTER TABLE orders
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'UTC';

ALTER TABLE outbox_event
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'UTC';

ALTER TABLE outbox_event
    ALTER COLUMN sent_at TYPE TIMESTAMPTZ
    USING CASE
        WHEN sent_at IS NULL THEN NULL
        ELSE sent_at AT TIME ZONE 'UTC'
    END;
