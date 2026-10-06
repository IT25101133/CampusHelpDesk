-- Adds the role and link columns used by ticket notifications.
-- user_id stays the recipient column and may be empty for a role-wide row.
-- Safe to run more than once on SQL Server.

IF COL_LENGTH('notifications', 'recipient_role') IS NULL
    ALTER TABLE notifications ADD recipient_role VARCHAR(20) NULL;

IF COL_LENGTH('notifications', 'link') IS NULL
    ALTER TABLE notifications ADD link VARCHAR(255) NULL;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('notifications')
      AND name = 'user_id'
      AND is_nullable = 0
)
    ALTER TABLE notifications ALTER COLUMN user_id BIGINT NULL;
