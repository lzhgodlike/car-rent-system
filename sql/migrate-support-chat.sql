ALTER TABLE sys_user DROP COLUMN IF EXISTS notification_enabled;

CREATE TABLE IF NOT EXISTS support_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_admin_id BIGINT,
    source_biz_type VARCHAR(40),
    source_biz_id BIGINT,
    last_message_preview VARCHAR(255),
    last_message_time DATETIME,
    user_unread_count INT NOT NULL DEFAULT 0,
    admin_unread_count INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_support_conversation_user_status (user_id, status),
    INDEX idx_support_conversation_status_time (status, last_message_time, id)
);

CREATE TABLE IF NOT EXISTS support_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    read_status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_support_message_conversation (conversation_id, id)
);
