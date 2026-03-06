-- V3: Выдачи и штрафы

CREATE TYPE loan_status AS ENUM ('ACTIVE', 'RETURNED', 'OVERDUE', 'LOST');
CREATE TYPE fine_status AS ENUM ('PENDING', 'PAID', 'WAIVED');
CREATE TYPE fine_reason AS ENUM ('OVERDUE', 'DAMAGE', 'LOSS');

CREATE TABLE loan (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    book_copy_id   UUID        NOT NULL REFERENCES book_copy(id),
    reader_id      UUID        NOT NULL REFERENCES reader(id),
    issued_at      TIMESTAMP   NOT NULL DEFAULT now(),
    due_date       DATE        NOT NULL,
    returned_at    TIMESTAMP,
    status         loan_status NOT NULL DEFAULT 'ACTIVE',
    issued_by      UUID        NOT NULL REFERENCES user_account(id),
    notes          TEXT
);

-- Гарантируем: один экземпляр не может быть выдан дважды одновременно
CREATE UNIQUE INDEX idx_loan_active_copy
    ON loan(book_copy_id)
    WHERE status IN ('ACTIVE', 'OVERDUE');

CREATE TABLE loan_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID        NOT NULL REFERENCES loan(id),
    previous_status loan_status,
    new_status      loan_status NOT NULL,
    changed_at      TIMESTAMP   NOT NULL DEFAULT now(),
    changed_by      UUID        NOT NULL REFERENCES user_account(id),
    comment         TEXT
);

CREATE TABLE fine (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id    UUID        NOT NULL UNIQUE REFERENCES loan(id),
    reason     fine_reason NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     fine_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    paid_at    TIMESTAMP
);

CREATE INDEX idx_loan_reader        ON loan(reader_id);
CREATE INDEX idx_loan_copy          ON loan(book_copy_id);
CREATE INDEX idx_loan_status        ON loan(status);
CREATE INDEX idx_loan_due_date      ON loan(due_date);
CREATE INDEX idx_loan_history_loan  ON loan_history(loan_id);
CREATE INDEX idx_fine_loan          ON fine(loan_id);
CREATE INDEX idx_fine_status        ON fine(status);
