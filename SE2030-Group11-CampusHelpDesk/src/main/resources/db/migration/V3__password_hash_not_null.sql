-- users.password_hash must store a BCrypt hash and must not be NULL.
-- PasswordHashEnforcer applies the same change on startup for SQL Server.
-- Run this in SQL Server Management Studio if you want the constraint before the app starts.
-- Plaintext values cannot be hashed by T-SQL; start the app once so PasswordHashEnforcer
-- rewrites them, then this ALTER is a no-op when the column is already NOT NULL.

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'users'
      AND COLUMN_NAME = 'password_hash'
      AND IS_NULLABLE = 'YES'
)
AND NOT EXISTS (
    SELECT 1 FROM users WHERE password_hash IS NULL OR password_hash NOT LIKE '$2%'
)
    ALTER TABLE users ALTER COLUMN password_hash VARCHAR(255) NOT NULL;
