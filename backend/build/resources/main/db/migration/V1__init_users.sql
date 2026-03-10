-- V1: UserAccount и Reader

CREATE TABLE user_account (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    login         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE reader (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id  UUID         NOT NULL UNIQUE REFERENCES user_account(id),
    full_name        VARCHAR(255) NOT NULL,
    phone            VARCHAR(20),
    email            VARCHAR(100),
    max_active_loans INT          NOT NULL DEFAULT 5,
    registered_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_account_login  ON user_account(login);
CREATE INDEX idx_reader_user_account ON reader(user_account_id);

-- Начальный пользователь-библиотекарь (пароль: admin123)
INSERT INTO user_account (id, login, password_hash, role, status) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'LIBRARIAN',
    'ACTIVE'
);