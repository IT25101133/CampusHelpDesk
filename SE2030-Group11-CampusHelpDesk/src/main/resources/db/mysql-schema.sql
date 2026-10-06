CREATE DATABASE IF NOT EXISTS campus_helpdesk;
USE campus_helpdesk;

-- ===================== AUTH & USERS (Nirasha) =====================
CREATE TABLE users (
    user_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('STUDENT','LECTURER','STAFF','ADMIN','DEPT_HEAD') NOT NULL,
    student_id      VARCHAR(20),
    department      VARCHAR(100),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ===================== CATEGORIZATION (Umer) =====================
CREATE TABLE ticket_categories (
    category_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    department      VARCHAR(100),
    sla_hours       INT DEFAULT 48,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===================== TICKETS (Kodagoda) =====================
CREATE TABLE tickets (
    ticket_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    category_id     BIGINT,
    priority        ENUM('LOW','MEDIUM','HIGH','CRITICAL') DEFAULT 'MEDIUM',
    status          ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED') DEFAULT 'OPEN',
    created_by      BIGINT NOT NULL,
    assigned_to     BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP NULL,
    sla_due_at      TIMESTAMP NULL,
    submitter_role  VARCHAR(20),
    delete_requested BOOLEAN DEFAULT FALSE,
    delete_approved BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (category_id) REFERENCES ticket_categories(category_id),
    FOREIGN KEY (created_by)  REFERENCES users(user_id),
    FOREIGN KEY (assigned_to) REFERENCES users(user_id)
);

CREATE TABLE ticket_attachments (
    attachment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE
);

-- ===================== COMMUNICATION (Arachchi) =====================
CREATE TABLE ticket_comments (
    comment_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    message         TEXT NOT NULL,
    is_internal     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)   REFERENCES users(user_id)
);

CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NULL,
    recipient_role  VARCHAR(20),
    ticket_id       BIGINT,
    message         VARCHAR(300) NOT NULL,
    link            VARCHAR(255),
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)   REFERENCES users(user_id),
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE SET NULL
);

-- ===================== KNOWLEDGE BASE (Weerasekara) =====================
CREATE TABLE kb_categories (
    kb_category_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE kb_articles (
    article_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    kb_category_id  BIGINT,
    created_by      BIGINT,
    views           INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_category_id) REFERENCES kb_categories(kb_category_id),
    FOREIGN KEY (created_by)     REFERENCES users(user_id)
);

-- ===================== REPORTING (Jayathilaka) =====================
CREATE TABLE audit_logs (
    log_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT,
    action          VARCHAR(200) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE reports (
    report_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(150) NOT NULL,
    department           VARCHAR(100),
    status               VARCHAR(24),
    priority             VARCHAR(16),
    ticket_count         BIGINT,
    resolved_count       BIGINT,
    avg_resolution_hours DOUBLE,
    created_by           BIGINT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);
