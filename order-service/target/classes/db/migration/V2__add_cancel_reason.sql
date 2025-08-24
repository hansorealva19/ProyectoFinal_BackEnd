-- Flyway migration: add cancel_reason column used by Order entity
-- Run this using your DB migration tool (Flyway) or execute manually against the order-service database.

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);
