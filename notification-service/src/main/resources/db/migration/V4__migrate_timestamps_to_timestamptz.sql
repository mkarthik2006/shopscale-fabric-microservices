ALTER TABLE inbox_event
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'UTC';

ALTER TABLE inbox_event
    ALTER COLUMN processed_at TYPE TIMESTAMPTZ
    USING CASE
        WHEN processed_at IS NULL THEN NULL
        ELSE processed_at AT TIME ZONE 'UTC'
    END;

ALTER TABLE processed_notifications
    ALTER COLUMN processed_at TYPE TIMESTAMPTZ
    USING CASE
        WHEN processed_at IS NULL THEN NULL
        ELSE processed_at AT TIME ZONE 'UTC'
    END;
