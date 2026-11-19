# Project Spec: Library Management REST API (Java + Spring Boot + PostgreSQL)

> Hand this file to your coding AI as the single source of truth for the project. Work through the milestones in order.

## 0. Instructions for the AI assistant

- You are helping a third-year software engineering student build a **portfolio project** for remote backend internship applications. The goal is not just working code, but a repository that looks professional and that the developer can **fully explain in an interview**.
- Work **one milestone at a time** (section 9). After each milestone: run the tests, make sure everything passes, and make a commit before continuing.
- Prefer **clear, simple, idiomatic code** over clever abstractions. Do not add features that are not in this spec. Add short comments only where they explain *why*, not *what*.
- Whenever you make a non-obvious design choice (locking strategy, token design, how holds work), add a short entry to `docs/DECISIONS.md` (context, decision, trade-offs). The developer will use these as interview talking points.
- Use **Conventional Commits** (`feat:`, `fix:`, `test:`, `docs:`, `chore:`) and small, focused commits. Use one branch per milestone merged by pull request if the developer wants a realistic history.
- If a requirement is ambiguous, pick the simplest reasonable interpretation, state it in `docs/DECISIONS.md`, and continue.

## 1. Goal and scope

A backend REST API for managing a library: a catalog of books, members who borrow and return them, a reservation queue for popular titles, and fines for late returns. There is **no frontend**; the API is documented with Swagger/OpenAPI and demonstrated through its docs page and a Postman/HTTP-file collection.

What this project should demonstrate: relational data modelling, a layered Spring Boot architecture, authentication and role-based authorization, business rules with transactions and concurrency control, schema migrations, automated testing against a real database, containerization, and CI.

## 2. Tech stack

| Area | Choice |
| --- | --- |
| Language / runtime | Java 21 or newer LTS |
| Framework | Spring Boot (latest stable): Web, Data JPA, Security, Validation, Actuator |
| Database | PostgreSQL |
| Migrations | Flyway (SQL migration files in `src/main/resources/db/migration`) |
| Auth | JWT access tokens (stateless), BCrypt password hashing |
| API docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`) |
| Build | Maven (with wrapper) |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, **Testcontainers (PostgreSQL)** |
| Containers | Multi-stage Dockerfile + `docker-compose.yml` (app + postgres) |
| CI | GitHub Actions: build and test on every push and pull request |

Do not use Lombok unless the developer asks; use Java records for DTOs.

## 3. Architecture

Package by feature, with layers inside each feature:

```
src/main/java/.../library/
  auth/          (controller, service, JWT utilities, security config)
  user/
  book/          (Book, Author, Category: entities, repositories, services, controllers, DTOs)
  loan/
  reservation/
  fine/
  report/
  common/        (error handling, pagination helpers, config properties, time provider)
```

Rules:
- Controllers only handle HTTP and validation. Services hold business logic and own transactions (`@Transactional`). Repositories only handle persistence.
- Never expose entities in API responses; map to DTOs.
- Inject a `Clock` (or time provider) wherever "now" or "today" is used, so time-dependent rules are testable.
- All business parameters (loan days, max loans, daily fine, hold duration) live in `@ConfigurationProperties` with sensible defaults in `application.yml`.
- Errors use the standard **RFC 7807 Problem Details** format via a global `@RestControllerAdvice`, with correct status codes (400, 401, 403, 404, 409, 422).

## 4. Data model

All tables use UUID primary keys (or bigint identity, but be consistent) and `created_at` / `updated_at` timestamps.

- **users**: id, email (unique), password_hash, full_name, role (`MEMBER` | `LIBRARIAN` | `ADMIN`), active.
- **authors**: id, name.
- **categories**: id, name (unique).
- **books**: id, isbn (unique, ISBN-13), title, description, publication_year, language, category_id, total_copies, available_copies, `version` (optimistic lock). Database check constraint: `0 <= available_copies <= total_copies`.
- **book_authors**: book_id, author_id (many-to-many).
- **loans**: id, book_id, user_id, borrowed_at, due_date, returned_at (nullable), renewals_count, status (`ACTIVE` | `RETURNED`).
- **reservations**: id, book_id, user_id, created_at, status (`WAITING` | `READY` | `FULFILLED` | `CANCELLED` | `EXPIRED`), ready_at, expires_at.
- **fines**: id, loan_id, user_id, amount, status (`UNPAID` | `PAID`), created_at, paid_at.

Add indexes for the main queries: `books(title)`, `loans(user_id, status)`, `loans(book_id, status)`, `reservations(book_id, status, created_at)`, `fines(user_id, status)`. An "overdue" loan is derived: `status = ACTIVE AND due_date < today`.

## 5. Business rules

**Borrowing**
- A member can have at most **5 active loans** (configurable) and cannot hold two active loans of the same book.
- A member cannot borrow if they have **any overdue active loan** or **any unpaid fine**.
- Default loan period: **14 days** from the borrow date.
- A copy can be borrowed only if one is available. If a member has a `READY` reservation for the book, they borrow using the copy being held for them (the reservation becomes `FULFILLED`).

**Concurrency (important)**
- Two members must never be able to borrow the last copy at the same time. Decrement availability atomically (a conditional update such as `UPDATE books SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0`, or a pessimistic lock, or optimistic locking with a retry). Pick one, document it in `docs/DECISIONS.md`, and **prove it with an integration test** that fires concurrent borrow requests at a book with one copy and asserts exactly one succeeds.

**Returning**
- Marks the loan `RETURNED`. If returned late, create a fine: `overdue_days * daily_rate` (default 2.00 per day, configurable).
- If someone is `WAITING` for that book, promote the oldest reservation to `READY` (expires after **3 days**, configurable) and keep the copy held (do not increase `available_copies`). Otherwise increase `available_copies`.

**Renewing**
- Up to **2 renewals** per loan, each extending the current due date by 14 days. Not allowed if the loan is overdue or if anyone is waiting or has a ready reservation for the book.

**Reservations**
- Allowed only when no copies are available. A member cannot reserve a book they already have on loan or already have a reservation for. Reservations are served first-come, first-served.
- A scheduled job (Spring `@Scheduled`, daily or hourly) expires `READY` reservations past `expires_at`, then promotes the next waiting member or releases the copy.

**Fines**
- A member with unpaid fines cannot borrow. A librarian marks a fine as paid (payment is simulated; no real payment provider).

**Deleting**
- A book cannot be deleted while it has active loans or open reservations. A user cannot be deleted; they can be deactivated.

## 6. API

Base path `/api/v1`. Pagination via `page`, `size`, `sort` and a consistent page response (`content`, `page`, `size`, `totalElements`, `totalPages`).

**Auth and users**
- `POST /auth/register`: creates a `MEMBER`.
- `POST /auth/login`: returns an access token.
- `GET /users/me`
- `PATCH /users/{id}/role` (ADMIN); `PATCH /users/{id}/active` (ADMIN)

**Catalog**
- `GET /books`: search and filter by `query` (title or author, case-insensitive), `author`, `category`, `available=true`, with pagination and sorting. Public.
- `GET /books/{id}`: public.
- `POST /books`, `PUT /books/{id}`, `DELETE /books/{id}` (LIBRARIAN or ADMIN)
- `GET /authors`, `POST /authors`, `GET /categories`, `POST /categories` (writes require LIBRARIAN or ADMIN)

**Loans**
- `POST /loans` with `{ "bookId": "..." }`: borrow (MEMBER).
- `POST /loans/{id}/return` (LIBRARIAN or ADMIN)
- `POST /loans/{id}/renew` (the loan's owner)
- `GET /loans/me?status=`: own loans.
- `GET /loans?overdue=true&userId=` (LIBRARIAN or ADMIN)

**Reservations**
- `POST /reservations` with `{ "bookId": "..." }`, `DELETE /reservations/{id}`, `GET /reservations/me`

**Fines**
- `GET /fines/me`; `POST /fines/{id}/pay` (LIBRARIAN or ADMIN)

**Reports (LIBRARIAN or ADMIN)**
- `GET /reports/overdue`: overdue loans with member and days late.
- `GET /reports/popular-books?days=30`: most borrowed books in the period.

**Operations**
- `GET /actuator/health` (public); other actuator endpoints restricted.

## 7. Security

- Stateless JWT in the `Authorization: Bearer` header; passwords hashed with BCrypt; tokens carry user id and role and have a short expiry.
- Authorization via roles (`@PreAuthorize` or request matchers). Members can only see and act on **their own** loans, reservations and fines (verify ownership in the service layer, not only in the controller).
- Secrets (JWT secret, DB password) come from environment variables, never from committed files. Provide a `.env.example`.
- Validate all input with Bean Validation (ISBN format, positive numbers, email format, password length).
- Seed data: a Flyway migration or a dev-profile seeder that creates an admin, a librarian, a member, and ~30 sample books so the demo works immediately. Document the seeded credentials in the README (dev only).

## 8. Testing and quality

- **Unit tests** (Mockito) for the service-layer business rules: borrow limit, overdue block, renewal rules, fine calculation, reservation promotion.
- **Integration tests** with Testcontainers PostgreSQL for repositories and the main API flows via MockMvc, including auth and role checks (401 vs 403).
- **Concurrency test** for the last-copy borrow (see section 5).
- Aim for meaningful coverage of the service layer (roughly 70%+), generated by JaCoCo and shown in CI output.
- CI must run the full test suite and fail the build on test failures.

## 9. Milestones

1. **Skeleton**: Maven project, config properties, Flyway baseline, Docker Compose with Postgres, health endpoint, global error handler, CI running a build.
2. **Auth and users**: registration, login, JWT, roles, security tests.
3. **Catalog**: authors, categories, books, search with filters and pagination, validation, tests.
4. **Loans**: borrow, return, renew with all rules, plus the concurrency test.
5. **Reservations**: queue, hold logic, scheduled expiry job, tests (use a controllable `Clock`).
6. **Fines and reports**: fine creation on late return, pay endpoint, overdue and popular-books reports.
7. **Hardening**: OpenAPI annotations, seed data, Dockerfile (multi-stage, non-root user), coverage report, cleanup.
8. **Docs and delivery**: README, `docs/DECISIONS.md`, Postman or `.http` collection, optional deployment.

## 10. README requirements

The README is what recruiters read first. It must include:
- One-paragraph description and a features list.
- Tech stack and a **simple architecture diagram** (Mermaid is fine) covering layers and the database.
- **Quick start**: clone, copy `.env.example`, `docker compose up`, and the Swagger URL, so it works in under five minutes.
- Seeded demo credentials (dev only) and a short example flow with `curl` or `.http` requests (login, search, borrow, return).
- The business rules summarized as a table.
- A "Design decisions" section linking to `docs/DECISIONS.md` (locking strategy, why JWT, why Flyway).
- How to run the tests, plus the CI badge.
- Optional: a live deployment link (free-tier hosts change their limits, so verify current terms) and a short GIF of the Swagger flow.

## 11. Definition of done

- `docker compose up` starts the full system and the Swagger UI works.
- All milestone features implemented; all tests pass locally and in CI.
- No secrets committed; `.env.example` present.
- README and `docs/DECISIONS.md` complete.
- Clean commit history using Conventional Commits.

## 12. Stretch goals (only after everything above is done)

Refresh tokens, email notifications (logged instead of sent), ETag/caching on catalog reads, rate limiting on login, Prometheus metrics, and a small admin dashboard.
