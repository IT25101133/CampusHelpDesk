-- SQL Server script for the account, report, and activity columns.
-- Hibernate ddl-auto=update also adds these on startup. Each statement is safe to run again.

IF COL_LENGTH('users', 'deactivated_at') IS NULL
    ALTER TABLE users ADD deactivated_at DATETIME2 NULL;
IF COL_LENGTH('users', 'deactivated_by') IS NULL
    ALTER TABLE users ADD deactivated_by BIGINT NULL;

IF COL_LENGTH('reports', 'updated_by') IS NULL
    ALTER TABLE reports ADD updated_by BIGINT NULL;
IF COL_LENGTH('reports', 'updated_at') IS NULL
    ALTER TABLE reports ADD updated_at DATETIME2 NULL;

IF OBJECT_ID('audit_event', 'U') IS NULL
BEGIN
    CREATE TABLE audit_event (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id       BIGINT NULL,
        action        VARCHAR(80) NOT NULL,
        ip_address    VARCHAR(64) NULL,
        user_agent    VARCHAR(255) NULL,
        target_entity VARCHAR(50) NULL,
        target_id     BIGINT NULL,
        metadata      VARCHAR(500) NULL,
        created_at    DATETIME2 NOT NULL CONSTRAINT DF_audit_event_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_audit_event_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    );
    CREATE INDEX idx_audit_event_user_created ON audit_event (user_id, created_at DESC);
END;

-- Attachment rows to compare with the upload folder.
-- Disk presence is checked by GET /api/admin/attachments/missing-files.
SELECT attachment_id, ticket_id, file_name, file_path, uploaded_at
FROM ticket_attachments
ORDER BY attachment_id;
