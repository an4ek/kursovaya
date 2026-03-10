-- V2: Каталог книг

CREATE TYPE book_copy_status AS ENUM ('AVAILABLE', 'LOANED', 'RESERVED', 'DAMAGED', 'WRITTEN_OFF');
CREATE TYPE book_copy_condition AS ENUM ('NEW', 'GOOD', 'SATISFACTORY', 'POOR');

CREATE TABLE book_title (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(500) NOT NULL,
    author      VARCHAR(255) NOT NULL,
    isbn        VARCHAR(20)  UNIQUE,
    genre       VARCHAR(100),
    year        INT,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE book_copy (
    id               UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    book_title_id    UUID              NOT NULL REFERENCES book_title(id),
    inventory_number VARCHAR(50)       NOT NULL UNIQUE,
    status           book_copy_status  NOT NULL DEFAULT 'AVAILABLE',
    condition        book_copy_condition NOT NULL DEFAULT 'GOOD',
    created_at       TIMESTAMP         NOT NULL DEFAULT now()
);

CREATE INDEX idx_book_title_isbn    ON book_title(isbn);
CREATE INDEX idx_book_title_author  ON book_title(author);
CREATE INDEX idx_book_copy_title    ON book_copy(book_title_id);
CREATE INDEX idx_book_copy_status   ON book_copy(status);
