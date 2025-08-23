-- Flyway migration: add payer_card column to payments
ALTER TABLE payments
ADD COLUMN payer_card VARCHAR(255) DEFAULT NULL;
