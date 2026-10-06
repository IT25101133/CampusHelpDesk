-- Demo password for every seeded account: password
-- BCrypt hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- 6 users (one account per product role, plus IT Support staff), 8 ticket categories,
-- 8 tickets across OPEN / IN_PROGRESS / RESOLVED / CLOSED, and 5 KB articles.

-- ===================== USERS (one per Role + IT Support) =====================
INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Nirasha Perera', 'admin@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'ADMIN', NULL, 'IT Services', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@sliit.lk');

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Jayathilaka Fernando', 'head@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'DEPT_HEAD', NULL, 'Faculty of Computing', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'head@sliit.lk');

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Perera Gunawardena', 'lecturer@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'LECTURER', NULL, 'Faculty of Computing', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'lecturer@sliit.lk');

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Umer Farook', 'staff@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'STAFF', NULL, 'Facilities', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'staff@sliit.lk');

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Arachchi Dissanayake', 'it.support@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'STAFF', NULL, 'IT Services', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'it.support@sliit.lk');

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Kodagoda Silva', 'student@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'STUDENT', 'IT20201234', 'Faculty of Computing', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student@sliit.lk');

-- Test login created on startup as well: test@sliit.lk / Test1234
-- Password hash is written by TestUserSeeder so it always matches the app encoder.

-- ===================== TICKET CATEGORIES (8) =====================
INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'IT Support', 'IT Services', 24
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'IT Support');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Academic', 'Faculty of Computing', 72
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Academic');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Library Services', 'Library', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Library Services');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Finance Office', 'Finance', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Finance Office');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Hostel', 'Hostel', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Hostel');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Facilities', 'Facilities', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Facilities');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Examinations', 'Examinations', 72
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Examinations');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'General', 'General', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'General');

-- ===================== TICKETS (8, mixed statuses) =====================
INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, sla_due_at)
SELECT 'Cannot sign in to the LMS',
       'Moodle returns an invalid credentials error after the password reset from last week. Student ID IT20201234.',
       'IN_PROGRESS', 'HIGH',
       (SELECT category_id FROM ticket_categories WHERE name = 'IT Support'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'it.support@sliit.lk'),
       TIMESTAMP '2026-09-08 09:15:00', TIMESTAMP '2026-09-09 14:02:00', TIMESTAMP '2026-09-09 09:15:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Cannot sign in to the LMS');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, sla_due_at)
SELECT 'Broken projector in Lab B-204',
       'The ceiling projector flickers and shuts off after about five minutes. Happened during the SE2030 lecture.',
       'OPEN', 'MEDIUM',
       (SELECT category_id FROM ticket_categories WHERE name = 'Facilities'),
       (SELECT user_id FROM users WHERE email = 'lecturer@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'staff@sliit.lk'),
       TIMESTAMP '2026-09-09 11:40:00', TIMESTAMP '2026-09-09 11:40:00', TIMESTAMP '2026-09-11 11:40:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Broken projector in Lab B-204');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, resolved_at, sla_due_at)
SELECT 'Request a fee payment receipt',
       'Need an official receipt for the semester 1 installment paid on 01 Sep 2026 for a visa extension.',
       'RESOLVED', 'LOW',
       (SELECT category_id FROM ticket_categories WHERE name = 'Finance Office'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'admin@sliit.lk'),
       TIMESTAMP '2026-09-04 16:20:00', TIMESTAMP '2026-09-06 10:05:00', TIMESTAMP '2026-09-06 10:05:00',
       TIMESTAMP '2026-09-06 16:20:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Request a fee payment receipt');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, resolved_at, sla_due_at)
SELECT 'Moodle assignment upload fails',
       'SE2030 Assignment 2 PDF will not upload. Moodle shows a 413 error after about 30 seconds.',
       'CLOSED', 'MEDIUM',
       (SELECT category_id FROM ticket_categories WHERE name = 'IT Support'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'it.support@sliit.lk'),
       TIMESTAMP '2026-08-28 10:05:00', TIMESTAMP '2026-08-30 16:40:00', TIMESTAMP '2026-08-30 16:40:00',
       TIMESTAMP '2026-08-29 10:05:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Moodle assignment upload fails');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, sla_due_at)
SELECT 'Air conditioning in Lecture Hall A-101',
       'The hall is above 30C by mid-morning. Last Friday lecture had to be shortened. Safety issue for a packed 200-seat room.',
       'OPEN', 'HIGH',
       (SELECT category_id FROM ticket_categories WHERE name = 'Facilities'),
       (SELECT user_id FROM users WHERE email = 'lecturer@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'staff@sliit.lk'),
       TIMESTAMP '2026-09-01 08:10:00', TIMESTAMP '2026-09-01 08:10:00', TIMESTAMP '2026-09-03 08:10:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Air conditioning in Lecture Hall A-101');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, sla_due_at)
SELECT 'Grade review for SE2030 CA',
       'CA1 mark on Courseweb is 12/20 but the returned script shows 16/20. Requesting a recorrection before the semester board.',
       'IN_PROGRESS', 'MEDIUM',
       (SELECT category_id FROM ticket_categories WHERE name = 'Academic'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'head@sliit.lk'),
       TIMESTAMP '2026-09-07 13:25:00', TIMESTAMP '2026-09-08 09:00:00', TIMESTAMP '2026-09-10 13:25:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Grade review for SE2030 CA');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, resolved_at, sla_due_at)
SELECT 'Scholarship stipend not received',
       'August merit scholarship of LKR 15,000 did not reach account 123-4-567. Payment advice was emailed on 20 Aug.',
       'RESOLVED', 'HIGH',
       (SELECT category_id FROM ticket_categories WHERE name = 'Finance Office'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'admin@sliit.lk'),
       TIMESTAMP '2026-08-25 11:00:00', TIMESTAMP '2026-08-27 15:30:00', TIMESTAMP '2026-08-27 15:30:00',
       TIMESTAMP '2026-08-27 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Scholarship stipend not received');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, resolved_at, sla_due_at)
SELECT 'Library access card not working',
       'Turnstile at the New Building library rejects the student card. Counter staff asked for a help-desk ticket before reprinting.',
       'CLOSED', 'LOW',
       (SELECT category_id FROM ticket_categories WHERE name = 'IT Support'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'it.support@sliit.lk'),
       TIMESTAMP '2026-08-20 09:45:00', TIMESTAMP '2026-08-21 12:10:00', TIMESTAMP '2026-08-21 12:10:00',
       TIMESTAMP '2026-08-21 09:45:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Library access card not working');

-- Existing rows: copy the requester's current role. New tickets set this at submit time.
UPDATE tickets
SET submitter_role = (SELECT role FROM users WHERE users.user_id = tickets.created_by)
WHERE submitter_role IS NULL;

-- ===================== COMMENTS =====================
INSERT INTO ticket_comments (ticket_id, user_id, message, is_internal, created_at)
SELECT t.ticket_id, u.user_id,
       'Reset token was resent. Please try LMS login again and reply here if it still fails.',
       FALSE, TIMESTAMP '2026-09-09 14:02:00'
FROM tickets t, users u
WHERE t.title = 'Cannot sign in to the LMS' AND u.email = 'it.support@sliit.lk'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_comments c
      WHERE c.ticket_id = t.ticket_id AND c.message LIKE 'Reset token was resent%'
  );

INSERT INTO ticket_comments (ticket_id, user_id, message, is_internal, created_at)
SELECT t.ticket_id, u.user_id,
       'Tried again this morning. Same invalid-credentials page after the reset email.',
       FALSE, TIMESTAMP '2026-09-10 08:20:00'
FROM tickets t, users u
WHERE t.title = 'Cannot sign in to the LMS' AND u.email = 'student@sliit.lk'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_comments c
      WHERE c.ticket_id = t.ticket_id AND c.message LIKE 'Tried again this morning%'
  );

INSERT INTO ticket_comments (ticket_id, user_id, message, is_internal, created_at)
SELECT t.ticket_id, u.user_id,
       'Technician booked for Friday 10:00. Spare lamp is in the Malabe stores.',
       FALSE, TIMESTAMP '2026-09-09 16:10:00'
FROM tickets t, users u
WHERE t.title = 'Broken projector in Lab B-204' AND u.email = 'staff@sliit.lk'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_comments c
      WHERE c.ticket_id = t.ticket_id AND c.message LIKE 'Technician booked%'
  );

INSERT INTO ticket_comments (ticket_id, user_id, message, is_internal, created_at)
SELECT t.ticket_id, u.user_id,
       'Receipt generated and emailed. Closing after student confirmation.',
       FALSE, TIMESTAMP '2026-09-06 10:05:00'
FROM tickets t, users u
WHERE t.title = 'Request a fee payment receipt' AND u.email = 'admin@sliit.lk'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_comments c
      WHERE c.ticket_id = t.ticket_id AND c.message LIKE 'Receipt generated%'
  );

INSERT INTO ticket_comments (ticket_id, user_id, message, is_internal, created_at)
SELECT t.ticket_id, u.user_id,
       'Courseweb mark updated to 16/20. Waiting on the student to confirm the portal.',
       FALSE, TIMESTAMP '2026-09-08 09:00:00'
FROM tickets t, users u
WHERE t.title = 'Grade review for SE2030 CA' AND u.email = 'head@sliit.lk'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_comments c
      WHERE c.ticket_id = t.ticket_id AND c.message LIKE 'Courseweb mark updated%'
  );

-- ===================== NOTIFICATIONS =====================
INSERT INTO notifications (user_id, ticket_id, message, is_read, created_at)
SELECT u.user_id, t.ticket_id, 'Ticket assigned to you: Cannot sign in to the LMS', FALSE, TIMESTAMP '2026-09-08 09:16:00'
FROM users u, tickets t
WHERE u.email = 'it.support@sliit.lk' AND t.title = 'Cannot sign in to the LMS'
  AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.ticket_id = t.ticket_id AND n.user_id = u.user_id);

INSERT INTO notifications (user_id, ticket_id, message, is_read, created_at)
SELECT u.user_id, t.ticket_id, 'IT Support replied on: Cannot sign in to the LMS', FALSE, TIMESTAMP '2026-09-09 14:03:00'
FROM users u, tickets t
WHERE u.email = 'student@sliit.lk' AND t.title = 'Cannot sign in to the LMS'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.ticket_id = t.ticket_id AND n.user_id = u.user_id AND n.message LIKE 'IT Support replied%'
  );

INSERT INTO notifications (user_id, ticket_id, message, is_read, created_at)
SELECT u.user_id, t.ticket_id, 'Ticket assigned to you: Broken projector in Lab B-204', FALSE, TIMESTAMP '2026-09-09 11:41:00'
FROM users u, tickets t
WHERE u.email = 'staff@sliit.lk' AND t.title = 'Broken projector in Lab B-204'
  AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.ticket_id = t.ticket_id AND n.user_id = u.user_id);

INSERT INTO notifications (user_id, ticket_id, message, is_read, created_at)
SELECT u.user_id, t.ticket_id, 'Ticket assigned to you: Grade review for SE2030 CA', TRUE, TIMESTAMP '2026-09-07 13:26:00'
FROM users u, tickets t
WHERE u.email = 'head@sliit.lk' AND t.title = 'Grade review for SE2030 CA'
  AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.ticket_id = t.ticket_id AND n.user_id = u.user_id);

INSERT INTO notifications (user_id, ticket_id, message, is_read, created_at)
SELECT u.user_id, t.ticket_id, 'Your fee receipt ticket was resolved', TRUE, TIMESTAMP '2026-09-06 10:06:00'
FROM users u, tickets t
WHERE u.email = 'student@sliit.lk' AND t.title = 'Request a fee payment receipt'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.ticket_id = t.ticket_id AND n.user_id = u.user_id AND n.message LIKE 'Your fee receipt%'
  );

-- ===================== KNOWLEDGE BASE (5 articles) =====================
INSERT INTO kb_categories (name)
SELECT 'Accounts & access'
WHERE NOT EXISTS (SELECT 1 FROM kb_categories WHERE name = 'Accounts & access');

INSERT INTO kb_categories (name)
SELECT 'Campus facilities'
WHERE NOT EXISTS (SELECT 1 FROM kb_categories WHERE name = 'Campus facilities');

INSERT INTO kb_categories (name)
SELECT 'Academic records'
WHERE NOT EXISTS (SELECT 1 FROM kb_categories WHERE name = 'Academic records');

INSERT INTO kb_categories (name)
SELECT 'Fees & payments'
WHERE NOT EXISTS (SELECT 1 FROM kb_categories WHERE name = 'Fees & payments');

INSERT INTO kb_articles (title, content, kb_category_id, created_by, views)
SELECT 'Reset your campus password',
       'Go to account.sliit.lk, choose Forgot password, and use your student email. The reset link expires in 30 minutes. If the email does not arrive, check the spam folder and then open an IT Support ticket.',
       (SELECT kb_category_id FROM kb_categories WHERE name = 'Accounts & access'),
       (SELECT user_id FROM users WHERE email = 'it.support@sliit.lk'),
       42
WHERE NOT EXISTS (SELECT 1 FROM kb_articles WHERE title = 'Reset your campus password');

INSERT INTO kb_articles (title, content, kb_category_id, created_by, views)
SELECT 'Report a classroom fault',
       'Note the building, room number, and whether the issue is electrical, furniture, or AV equipment. Submit a Facilities ticket with a photo if possible. Urgent safety issues should also be called in to Facilities on 011-754-4810.',
       (SELECT kb_category_id FROM kb_categories WHERE name = 'Campus facilities'),
       (SELECT user_id FROM users WHERE email = 'staff@sliit.lk'),
       18
WHERE NOT EXISTS (SELECT 1 FROM kb_articles WHERE title = 'Report a classroom fault');

INSERT INTO kb_articles (title, content, kb_category_id, created_by, views)
SELECT 'Connect to campus Wi-Fi',
       'Choose the eduroam network, enter your full SLIIT email and campus password, and accept the certificate. Staff laptops that previously used SLIIT-Staff should forget that network first. If authentication loops, forget the network and retry or open an IT Support ticket.',
       (SELECT kb_category_id FROM kb_categories WHERE name = 'Accounts & access'),
       (SELECT user_id FROM users WHERE email = 'it.support@sliit.lk'),
       31
WHERE NOT EXISTS (SELECT 1 FROM kb_articles WHERE title = 'Connect to campus Wi-Fi');

INSERT INTO kb_articles (title, content, kb_category_id, created_by, views)
SELECT 'Request an official transcript',
       'Transcripts are issued by the Faculty Office, not IT. Submit an Academic ticket with your registration number, intake, and whether you need a digital or sealed paper copy. Allow three working days during the semester and five days during exams.',
       (SELECT kb_category_id FROM kb_categories WHERE name = 'Academic records'),
       (SELECT user_id FROM users WHERE email = 'head@sliit.lk'),
       24
WHERE NOT EXISTS (SELECT 1 FROM kb_articles WHERE title = 'Request an official transcript');

INSERT INTO kb_articles (title, content, kb_category_id, created_by, views)
SELECT 'Download a fee payment receipt',
       'Sign in to the student portal, open Finance, and choose Payment history. Official VAT receipts for visas must be requested through a Finance ticket with the payment date and bank reference. Self-printed screenshots are not accepted by the embassy desk.',
       (SELECT kb_category_id FROM kb_categories WHERE name = 'Fees & payments'),
       (SELECT user_id FROM users WHERE email = 'admin@sliit.lk'),
       37
WHERE NOT EXISTS (SELECT 1 FROM kb_articles WHERE title = 'Download a fee payment receipt');

-- ===================== AUDIT LOG =====================
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, created_at)
SELECT u.user_id, 'Created ticket', 'TICKET', t.ticket_id, TIMESTAMP '2026-09-08 09:15:00'
FROM users u, tickets t
WHERE u.email = 'student@sliit.lk' AND t.title = 'Cannot sign in to the LMS'
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type = 'TICKET' AND a.entity_id = t.ticket_id AND a.action = 'Created ticket');

INSERT INTO audit_logs (user_id, action, entity_type, entity_id, created_at)
SELECT u.user_id, 'Assigned ticket to IT Support', 'TICKET', t.ticket_id, TIMESTAMP '2026-09-08 09:16:00'
FROM users u, tickets t
WHERE u.email = 'it.support@sliit.lk' AND t.title = 'Cannot sign in to the LMS'
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type = 'TICKET' AND a.entity_id = t.ticket_id AND a.action = 'Assigned ticket to IT Support');

INSERT INTO audit_logs (user_id, action, entity_type, entity_id, created_at)
SELECT u.user_id, 'Created ticket', 'TICKET', t.ticket_id, TIMESTAMP '2026-09-04 16:20:00'
FROM users u, tickets t
WHERE u.email = 'student@sliit.lk' AND t.title = 'Request a fee payment receipt'
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type = 'TICKET' AND a.entity_id = t.ticket_id AND a.action = 'Created ticket');

INSERT INTO audit_logs (user_id, action, entity_type, entity_id, created_at)
SELECT u.user_id, 'Updated ticket status from OPEN to RESOLVED', 'TICKET', t.ticket_id, TIMESTAMP '2026-09-06 10:05:00'
FROM users u, tickets t
WHERE u.email = 'admin@sliit.lk' AND t.title = 'Request a fee payment receipt'
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type = 'TICKET' AND a.entity_id = t.ticket_id AND a.action = 'Updated ticket status from OPEN to RESOLVED');

INSERT INTO audit_logs (user_id, action, entity_type, entity_id, created_at)
SELECT u.user_id, 'Commented on ticket', 'TICKET', t.ticket_id, TIMESTAMP '2026-09-08 09:00:00'
FROM users u, tickets t
WHERE u.email = 'head@sliit.lk' AND t.title = 'Grade review for SE2030 CA'
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type = 'TICKET' AND a.entity_id = t.ticket_id AND a.action = 'Commented on ticket');
