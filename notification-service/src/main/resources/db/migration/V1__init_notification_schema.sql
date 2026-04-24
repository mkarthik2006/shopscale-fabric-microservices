CREATE TABLE IF NOT EXISTS inbox_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS processed_notifications (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255),
    processed_at TIMESTAMP
);
