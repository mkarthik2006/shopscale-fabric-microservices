CREATE TABLE IF NOT EXISTS inventory (
    sku VARCHAR(64) PRIMARY KEY,
    stock INTEGER NOT NULL,
    version BIGINT,
    CONSTRAINT chk_inventory_stock_non_negative CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS inbox_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    status VARCHAR(255)
);
