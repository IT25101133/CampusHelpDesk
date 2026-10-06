-- Campus Help Desk schema (H2 MySQL mode and MySQL).
-- For MySQL Workbench, run db/mysql-schema.sql first (creates campus_helpdesk).

-- ===================== AUTH & USERS (Nirasha) =====================
CREATE TABLE IF NOT EXISTS users (
    user_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    student_id      VARCHAR(20),
    department      VARCHAR(100),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===================== CATEGORIZATION (Umer) =====================
CREATE TABLE IF NOT EXISTS ticket_categories (
    category_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    department      VARCHAR(100),
    sla_hours       INT DEFAULT 48,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===================== TICKETS (Kodagoda) =====================
CREATE TABLE IF NOT EXISTS tickets (
    ticket_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    category_id     BIGINT,
    priority        VARCHAR(16) DEFAULT 'MEDIUM',
    status          VARCHAR(24) DEFAULT 'OPEN',
    created_by      BIGINT NOT NULL,
    assigned_to     BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP NULL,
    FOREIGN KEY (category_id) REFERENCES ticket_categories(category_id),
    FOREIGN KEY (created_by)  REFERENCES users(user_id),
    FOREIGN KEY (assigned_to) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS ticket_attachments (
    attachment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE
);

-- ===================== COMMUNICATION (Arachchi) =====================
CREATE TABLE IF NOT EXISTS ticket_comments (
    comment_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    message         TEXT NOT NULL,
    is_internal     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)   REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    ticket_id       BIGINT,
    message         VARCHAR(300) NOT NULL,
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)   REFERENCES users(user_id),
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE SET NULL
);

-- ===================== KNOWLEDGE BASE (Weerasekara) =====================
CREATE TABLE IF NOT EXISTS kb_categories (
    kb_category_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS kb_articles (
    article_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    kb_category_id  BIGINT,
    created_by      BIGINT,
    views           INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_category_id) REFERENCES kb_categories(kb_category_id),
    FOREIGN KEY (created_by)     REFERENCES users(user_id)
);

-- ===================== REPORTING (Jayathilaka) =====================
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT,
    action          VARCHAR(200) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_tickets_created_by  ON tickets (created_by);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_to ON tickets (assigned_to);
CREATE INDEX IF NOT EXISTS idx_tickets_status      ON tickets (status);
CREATE INDEX IF NOT EXISTS idx_notif_user          ON notifications (user_id, is_read);
