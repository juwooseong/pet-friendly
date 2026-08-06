-- Flyway V3__add_user_withdraw_at.sql: Add withdraw_at column to users table for Soft Delete audit tracking

ALTER TABLE users ADD COLUMN IF NOT EXISTS withdraw_at TIMESTAMP WITH TIME ZONE;
