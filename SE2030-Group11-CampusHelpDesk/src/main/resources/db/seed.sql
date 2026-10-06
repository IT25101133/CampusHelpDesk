-- Demo password for every seeded account: password
-- BCrypt hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Nirasha Perera', 'admin@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'ADMIN', NULL, 'IT Services', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@sliit.lk');

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
SELECT 'Kodagoda Silva', 'student@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'STUDENT', 'IT20201234', 'Faculty of Computing', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student@sliit.lk');

INSERT INTO users (full_name, email, password_hash, role, student_id, department, is_active)
SELECT 'Weerasekara Jayasuriya', 'weerasekara@sliit.lk',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'STUDENT', 'EN20204567', 'Faculty of Engineering', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'weerasekara@sliit.lk');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'IT Support', 'IT Services', 24
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'IT Support');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Facilities', 'Facilities', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Facilities');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Academic', 'Faculty of Computing', 72
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Academic');

INSERT INTO ticket_categories (name, department, sla_hours)
SELECT 'Finance', 'IT Services', 48
WHERE NOT EXISTS (SELECT 1 FROM ticket_categories WHERE name = 'Finance');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at)
SELECT 'Cannot sign in to the LMS',
       'Moodle returns an invalid credentials error after the password reset from last week. Student ID IT20201234.',
       'IN_PROGRESS', 'HIGH',
       (SELECT category_id FROM ticket_categories WHERE name = 'IT Support'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'it.support@sliit.lk'),
       TIMESTAMP '2026-09-08 09:15:00', TIMESTAMP '2026-09-09 14:02:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Cannot sign in to the LMS');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at)
SELECT 'Broken projector in Lab B-204',
       'The ceiling projector flickers and shuts off after about five minutes. Happened during the SE2030 lecture.',
       'OPEN', 'MEDIUM',
       (SELECT category_id FROM ticket_categories WHERE name = 'Facilities'),
       (SELECT user_id FROM users WHERE email = 'weerasekara@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'staff@sliit.lk'),
       TIMESTAMP '2026-09-09 11:40:00', TIMESTAMP '2026-09-09 11:40:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Broken projector in Lab B-204');

INSERT INTO tickets (title, description, status, priority, category_id, created_by, assigned_to, created_at, updated_at, resolved_at)
SELECT 'Request a fee payment receipt',
       'Need an official receipt for the semester 1 installment paid on 01 Sep 2026 for a visa extension.',
       'RESOLVED', 'LOW',
       (SELECT category_id FROM ticket_categories WHERE name = 'Finance'),
       (SELECT user_id FROM users WHERE email = 'student@sliit.lk'),
       (SELECT user_id FROM users WHERE email = 'admin@sliit.lk'),
       TIMESTAMP '2026-09-04 16:20:00', TIMESTAMP '2026-09-06 10:05:00', TIMESTAMP '2026-09-06 10:05:00'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Request a fee payment receipt');

INSERT INTO ticket_comments (ticket_id, user_id, message, is_internal, created_at)
SELECT t.ticket_id, u.user_id,
       'Reset token was resent. Please try LMS login again and reply here if it still fails.',
       FALSE, TIMESTAMP '2026-09-09 14:02:00'
FROM tickets t, users u
WHERE t.title = 'Cannot sign in to the LMS' AND u.email = 'it.support@sliit.lk'
  AND NOT EXISTS (SELECT 1 FROM ticket_comments c WHERE c.ticket_id = t.ticket_id);

INSERT INTO notifications (user_id, ticket_id, message, is_read, created_at)
SELECT u.user_id, t.ticket_id, 'Ticket assigned to you: Cannot sign in to the LMS', FALSE, TIMESTAMP '2026-09-08 09:16:00'
FROM users u, tickets t
WHERE u.email = 'it.support@sliit.lk' AND t.title = 'Cannot sign in to the LMS'
  AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.ticket_id = t.ticket_id AND n.user_id = u.user_id);

INSERT INTO kb_categories (name)
SELECT 'Accounts & access'
WHERE NOT EXISTS (SELECT 1 FROM kb_categories WHERE name = 'Accounts & access');

INSERT INTO kb_categories (name)
SELECT 'Campus facilities'
WHERE NOT EXISTS (SELECT 1 FROM kb_categories WHERE name = 'Campus facilities');

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
