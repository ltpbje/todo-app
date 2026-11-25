-- MySQL schema for todo-app (JPA ddl-auto=update also creates tables; this is for manual setup)
CREATE DATABASE IF NOT EXISTS todo_db DEFAULT CHARACTER SET utf8mb4;
USE todo_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    due_date DATE,
    sort_order INT NOT NULL DEFAULT 0,
    pomodoro_count INT NOT NULL DEFAULT 0,
    last_pomodoro_at DATETIME,
    completed_at DATETIME,
    category VARCHAR(40),
    priority INT NOT NULL DEFAULT 0,
    remind_at DATETIME,
    repeat_rule VARCHAR(16) DEFAULT 'none',
    parent_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_todos_user (user_id),
    INDEX idx_todos_completed (completed)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pomodoro_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    todo_id BIGINT,
    todo_title VARCHAR(200),
    duration_sec INT NOT NULL,
    ended_at DATETIME NOT NULL,
    INDEX idx_pomodoro_user (user_id, ended_at)
) ENGINE=InnoDB;
