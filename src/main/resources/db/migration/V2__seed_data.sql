-- V2__seed_data.sql: Demonstration and development seed data

-- Seed Users (default password for all: password123)
-- BCrypt Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
INSERT INTO users (id, email, password_hash, full_name, role, active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'admin@library.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'System Administrator', 'ADMIN', true, NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'librarian@library.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Head Librarian', 'LIBRARIAN', true, NOW(), NOW()),
('33333333-3333-3333-3333-333333333333', 'member@library.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Alice Member', 'MEMBER', true, NOW(), NOW()),
('44444444-4444-4444-4444-444444444444', 'bob.member@library.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Bob Member', 'MEMBER', true, NOW(), NOW());

-- Seed Categories
INSERT INTO categories (id, name, created_at, updated_at) VALUES
('c1111111-1111-1111-1111-111111111111', 'Computer Science', NOW(), NOW()),
('c2222222-2222-2222-2222-222222222222', 'Fiction & Literature', NOW(), NOW()),
('c3333333-3333-3333-3333-333333333333', 'Psychology & Philosophy', NOW(), NOW()),
('c4444444-4444-4444-4444-444444444444', 'History & Biography', NOW(), NOW()),
('c5555555-5555-5555-5555-555555555555', 'Science & Nature', NOW(), NOW());

-- Seed Authors
INSERT INTO authors (id, name, created_at, updated_at) VALUES
('a1111111-1111-1111-1111-111111111111', 'Joshua Bloch', NOW(), NOW()),
('a2222222-2222-2222-2222-222222222222', 'Robert C. Martin', NOW(), NOW()),
('a3333333-3333-3333-3333-333333333333', 'Martin Fowler', NOW(), NOW()),
('a4444444-4444-4444-4444-444444444444', 'Eric Evans', NOW(), NOW()),
('a5555555-5555-5555-5555-555555555555', 'George Orwell', NOW(), NOW()),
('a6666666-6666-6666-6666-666666666666', 'Frank Herbert', NOW(), NOW()),
('a7777777-7777-7777-7777-777777777777', 'Daniel Kahneman', NOW(), NOW()),
('a8888888-8888-8888-8888-888888888888', 'Yuval Noah Harari', NOW(), NOW());

-- Seed Books
INSERT INTO books (id, isbn, title, description, publication_year, language, category_id, total_copies, available_copies, version, created_at, updated_at) VALUES
('b1111111-1111-1111-1111-111111111111', '9780134685991', 'Effective Java (3rd Edition)', 'The definitive guide to Java platform best practices.', 2018, 'English', 'c1111111-1111-1111-1111-111111111111', 5, 5, 0, NOW(), NOW()),
('b2222222-2222-2222-2222-222222222222', '9780132350884', 'Clean Code', 'A handbook of agile software craftsmanship.', 2008, 'English', 'c1111111-1111-1111-1111-111111111111', 4, 4, 0, NOW(), NOW()),
('b3333333-3333-3333-3333-333333333333', '9780134757599', 'Refactoring (2nd Edition)', 'Improving the design of existing code.', 2018, 'English', 'c1111111-1111-1111-1111-111111111111', 3, 3, 0, NOW(), NOW()),
('b4444444-4444-4444-4444-444444444444', '9780321125217', 'Domain-Driven Design', 'Tackling complexity in the heart of software.', 2003, 'English', 'c1111111-1111-1111-1111-111111111111', 3, 3, 0, NOW(), NOW()),
('b5555555-5555-5555-5555-555555555555', '9780451524935', '1984', 'A dystopian classic novel on totalitarian surveillance and individualism.', 1949, 'English', 'c2222222-2222-2222-2222-222222222222', 6, 6, 0, NOW(), NOW()),
('b6666666-6666-6666-6666-666666666666', '9780441013593', 'Dune', 'Science fiction epic set on the desert planet Arrakis.', 1965, 'English', 'c2222222-2222-2222-2222-222222222222', 4, 4, 0, NOW(), NOW()),
('b7777777-7777-7777-7777-777777777777', '9780374533557', 'Thinking, Fast and Slow', 'Insightful exploration of the two systems driving human judgment and choice.', 2011, 'English', 'c3333333-3333-3333-3333-333333333333', 5, 5, 0, NOW(), NOW()),
('b8888888-8888-8888-8888-888888888888', '9780062316097', 'Sapiens: A Brief History of Humankind', 'The history and evolution of our species from the Stone Age to modern times.', 2014, 'English', 'c4444444-4444-4444-4444-444444444444', 4, 4, 0, NOW(), NOW()),
('b9999999-9999-9999-9999-999999999999', '9780134494166', 'Clean Architecture', 'A craftsman guide to software structure and design.', 2017, 'English', 'c1111111-1111-1111-1111-111111111111', 3, 3, 0, NOW(), NOW());

-- Seed Book Authors Many-to-Many Relationships
INSERT INTO book_authors (book_id, author_id) VALUES
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111'),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222'),
('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333'),
('b4444444-4444-4444-4444-444444444444', 'a4444444-4444-4444-4444-444444444444'),
('b5555555-5555-5555-5555-555555555555', 'a5555555-5555-5555-5555-555555555555'),
('b6666666-6666-6666-6666-666666666666', 'a6666666-6666-6666-6666-666666666666'),
('b7777777-7777-7777-7777-777777777777', 'a7777777-7777-7777-7777-777777777777'),
('b8888888-8888-8888-8888-888888888888', 'a8888888-8888-8888-8888-888888888888'),
('b9999999-9999-9999-9999-999999999999', 'a2222222-2222-2222-2222-222222222222');
