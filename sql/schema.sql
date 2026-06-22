DROP DATABASE IF EXISTS car_rental_system;
CREATE DATABASE car_rental_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE car_rental_system;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    id_card VARCHAR(30),
    gender VARCHAR(10),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS car_type;
CREATE TABLE car_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS car_info;
CREATE TABLE car_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    car_no VARCHAR(50) NOT NULL UNIQUE,
    type_id BIGINT NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    plate_number VARCHAR(30) NOT NULL UNIQUE,
    day_price DECIMAL(10,2) NOT NULL,
    mileage INT NOT NULL DEFAULT 0,
    province VARCHAR(50),
    city VARCHAR(50),
    detail_address VARCHAR(255),
    pickup_address VARCHAR(255),
    car_image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS car_image;
CREATE TABLE car_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    car_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    source_type VARCHAR(20) NOT NULL DEFAULT 'SERVER',
    origin_url VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_car_image_car_id_sort (car_id, sort_order, id)
);

DROP TABLE IF EXISTS rent_order;
CREATE TABLE rent_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    rent_date DATE NOT NULL,
    expected_return_date DATE NOT NULL,
    actual_return_date DATE,
    rent_days INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PICKUP',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    payment_method VARCHAR(20),
    payment_time DATETIME,
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS return_order;
CREATE TABLE return_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rent_order_id BIGINT NOT NULL UNIQUE,
    actual_return_time DATETIME NOT NULL,
    actual_mileage INT NOT NULL,
    damage_desc VARCHAR(255),
    extra_fee DECIMAL(10,2) DEFAULT 0.00,
    extra_fee_payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    operator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS fault_report;
CREATE TABLE fault_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    fault_content VARCHAR(255) NOT NULL,
    fault_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    handle_result VARCHAR(255),
    report_time DATETIME NOT NULL,
    handle_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS user_favorite;
CREATE TABLE user_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_car (user_id, car_id)
);

DROP TABLE IF EXISTS message_notice;
CREATE TABLE message_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    sender_id BIGINT,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    message_type VARCHAR(40) NOT NULL,
    biz_type VARCHAR(40),
    biz_id BIGINT,
    read_status TINYINT NOT NULL DEFAULT 0,
    read_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_notice_receiver (receiver_id, id),
    INDEX idx_message_notice_unread (receiver_id, read_status, id)
);

DROP TABLE IF EXISTS support_conversation;
CREATE TABLE support_conversation (
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

DROP TABLE IF EXISTS support_message;
CREATE TABLE support_message (
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
