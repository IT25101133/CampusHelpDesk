-- Additive migration for existing campus_helpdesk databases (SQL Server).
-- Hibernate ddl-auto=update also adds these columns from the Ticket entity.
-- submitter_role is backfilled from the requester's current users.role.
-- Tickets that predate this column and have no requester role stay NULL until
-- TicketSubmitterRoleBackfill stores STUDENT as the documented fallback.
-- delete_requested / delete_approved default to false (NULL is treated as false).

IF COL_LENGTH('tickets', 'submitter_role') IS NULL
    ALTER TABLE tickets ADD submitter_role VARCHAR(20) NULL;

IF COL_LENGTH('tickets', 'delete_requested') IS NULL
    ALTER TABLE tickets ADD delete_requested BIT NULL;

IF COL_LENGTH('tickets', 'delete_approved') IS NULL
    ALTER TABLE tickets ADD delete_approved BIT NULL;

UPDATE t
SET submitter_role = u.role
FROM tickets t
INNER JOIN users u ON u.user_id = t.created_by
WHERE t.submitter_role IS NULL;

UPDATE tickets SET delete_requested = 0 WHERE delete_requested IS NULL;
UPDATE tickets SET delete_approved = 0 WHERE delete_approved IS NULL;
