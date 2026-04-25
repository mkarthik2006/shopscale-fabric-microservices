-- ──────────────────────────────────────────────────────
-- ShopScale Fabric — Multi-Database Initialization
-- orderdb is auto-created via POSTGRES_DB env var
-- This script creates additional databases required by
-- other microservices in the fabric.
-- ──────────────────────────────────────────────────────

SELECT 'CREATE DATABASE inventorydb'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'inventorydb')\gexec
SELECT format('GRANT ALL PRIVILEGES ON DATABASE inventorydb TO %I', current_user)\gexec

SELECT 'CREATE DATABASE notificationdb'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'notificationdb')\gexec
SELECT format('GRANT ALL PRIVILEGES ON DATABASE notificationdb TO %I', current_user)\gexec

SELECT 'CREATE DATABASE keycloakdb'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'keycloakdb')\gexec
SELECT format('GRANT ALL PRIVILEGES ON DATABASE keycloakdb TO %I', current_user)\gexec