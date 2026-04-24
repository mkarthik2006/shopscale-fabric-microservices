ALTER TABLE inbox_event
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'UTC';

ALTER TABLE inbox_event
    ALTER COLUMN processed_at TYPE TIMESTAMPTZ
    USING CASE
        WHEN processed_at IS NULL THEN NULL
        ELSE processed_at AT TIME ZONE 'UTC'
    END;

ALTER TABLE processed_events
    ALTER COLUMN processed_at TYPE TIMESTAMPTZ
    USING processed_at AT TIME ZONE 'UTC';

ALTER TABLE compensation_outbox
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'UTC';

ALTER TABLE compensation_outbox
    ALTER COLUMN sent_at TYPE TIMESTAMPTZ
    USING CASE
        WHEN sent_at IS NULL THEN NULL
        ELSE sent_at AT TIME ZONE 'UTC'
    END;
