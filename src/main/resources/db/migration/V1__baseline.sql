-- V1__baseline.sql: Initial schema for Library Management API

-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('MEMBER', 'LIBRARIAN', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Authors Table
CREATE TABLE authors (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Categories Table
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Books Table
CREATE TABLE books (
    id UUID PRIMARY KEY,
    isbn VARCHAR(13) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    publication_year INT NOT NULL,
    language VARCHAR(50) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories(id),
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

-- Book Authors (Many-to-Many Join Table)
CREATE TABLE book_authors (
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES authors(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

-- Loans Table
CREATE TABLE loans (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books(id),
    user_id UUID NOT NULL REFERENCES users(id),
    borrowed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    due_date DATE NOT NULL,
    returned_at TIMESTAMPTZ,
    renewals_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'RETURNED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Reservations Table
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books(id),
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL CHECK (status IN ('WAITING', 'READY', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ready_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Fines Table
CREATE TABLE fines (
    id UUID PRIMARY KEY,
    loan_id UUID NOT NULL REFERENCES loans(id),
    user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('UNPAID', 'PAID')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance & query optimization
CREATE INDEX idx_books_title ON books(LOWER(title));
CREATE INDEX idx_loans_user_status ON loans(user_id, status);
CREATE INDEX idx_loans_book_status ON loans(book_id, status);
CREATE INDEX idx_reservations_book_status_created ON reservations(book_id, status, created_at);
CREATE INDEX idx_fines_user_status ON fines(user_id, status);
