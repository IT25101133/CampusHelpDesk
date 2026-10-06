-- Lists every ticket attachment row. SQL Server cannot see the IntelliJ upload folder,
-- so compare file_path with disk via GET /api/admin/attachments/missing-files.
-- A row here whose file is absent is a missing-file attachment.
-- A file under src/main/resources/static/uploads that is not in this result is an orphan file.
SELECT attachment_id, ticket_id, file_name, file_path, uploaded_at
FROM ticket_attachments
ORDER BY attachment_id;
