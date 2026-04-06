-- ──────────────────────────────────────────────────────
-- ShopScale Fabric — Multi-Database Initialization
-- orderdb is auto-created via POSTGRES_DB env var
-- This script creates additional databases required by
-- other microservices in the fabric.
-- ──────────────────────────────────────────────────────

CREATE DATABASE inventorydb;
GRANT ALL PRIVILEGES ON DATABASE inventorydb TO shopscale;

CREATE DATABASE notificationdb;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO shopscale;