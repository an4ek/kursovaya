-- V3: Выдачи и штрафы
CREATE TABLE loan (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    book_copy_id   UUID        NOT NULL REFERENCES book_copy(id),
    reader_id      UUID        NOT NULL REFERENCES reader(id),
    issued_at      TIMESTAMP   NOT NULL DEFAULT now(),
    due_date       DATE        NOT NULL,
    returned_at    TIMESTAMP,
    status         VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    issued_by      UUID        NOT NULL REFERENCES user_account(id),
    notes          TEXT
);
CREATE UNIQUE INDEX idx_loan_active_copy
    ON loan(book_copy_id)
    WHERE status IN ('ACTIVE', 'OVERDUE');
CREATE TABLE loan_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID        NOT NULL REFERENCES loan(id),
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    changed_at      TIMESTAMP   NOT NULL DEFAULT now(),
    changed_by      UUID        NOT NULL REFERENCES user_account(id),
    comment         TEXT
);
CREATE TABLE fine (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id    UUID          NOT NULL UNIQUE REFERENCES loan(id),
    reason     VARCHAR(50)   NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    paid_at    TIMESTAMP
);
CREATE INDEX idx_loan_reader        ON loan(reader_id);
CREATE INDEX idx_loan_copy          ON loan(book_copy_id);
CREATE INDEX idx_loan_status        ON loan(status);
CREATE INDEX idx_loan_due_date      ON loan(due_date);
CREATE INDEX idx_loan_history_loan  ON loan_history(loan_id);
CREATE INDEX idx_fine_loan          ON fine(loan_id);
CREATE INDEX idx_fine_status        ON fine(status);
