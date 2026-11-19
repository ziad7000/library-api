# Architecture Decision Records (ADR)

This document tracks significant technical and architectural choices made during the development of the Library Management API. Each record provides context, decision rationale, and evaluated trade-offs for technical discussion.

---

## ADR-001: Primary Key Strategy (UUID vs. Auto-Increment Bigint)

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
When designing the relational schema for books, users, loans, reservations, and fines, we evaluated primary key strategies: synthetic auto-increment integers (`BIGSERIAL` / identity) vs. universally unique identifiers (UUIDv4).

### Decision
Use **UUID primary keys** across all primary tables (`users`, `books`, `authors`, `categories`, `loans`, `reservations`, `fines`).

### Rationale
1. **Security & Obfuscation:** Auto-increment IDs leak volume metrics and allow enumeration attacks (e.g. `GET /users/1`, `GET /users/2`). UUIDs prevent unauthorized enumeration across public and member-facing endpoints.
2. **Distributed ID Generation:** UUIDs can be generated on the client or application layer prior to database persistence without requiring a round-trip sequence fetch.
3. **Multi-environment Portability:** Easier data migrations and replication without primary key collisions.

### Trade-offs & Mitigations
- *Storage overhead:* UUIDs require 16 bytes compared to 8 bytes for `BIGINT`. Given our target scale (tens of thousands of titles and transactions), storage impact is negligible.
- *B-tree index fragmentation:* Random UUIDv4 insertion can cause page splits. B-tree performance remains well within sub-millisecond ranges for our expected dataset size, and selective indexing is applied only on frequently queried attributes.

---

## ADR-002: Stateless JWT Authentication & Short Expiration

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
API consumers require secure authentication for personal loan operations, administrative actions, and role-based permissions. We evaluated server-side stateful HTTP sessions (with Redis / in-memory session tables) vs. stateless JSON Web Tokens (JWT).

### Decision
Implement **stateless HMAC-SHA256 signed JWT access tokens** transmitted via the `Authorization: Bearer <token>` header.

### Rationale
1. **Stateless Scalability:** The backend service remains completely stateless, enabling horizontal pod/container scaling without session clustering or distributed cache infrastructure.
2. **Standard REST Compliance:** Decouples the client from cookie-based browser sessions, fitting mobile apps, single-page applications, and CLI/API consumers.
3. **Self-Contained Claims:** User ID and Role (`MEMBER`, `LIBRARIAN`, `ADMIN`) are encoded directly into the token payload, eliminating database lookups on every authorization check.

### Trade-offs & Mitigations
- *Immediate Revocation:* Stateless tokens cannot be revoked before expiration without maintaining a blacklist. Mitigated by setting short token lifetimes (configurable via `JWT_EXPIRATION_MS`, defaulting to 24 hours in development and reducible in production).

---

## ADR-003: Password Hashing with BCrypt

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
User credentials must be stored safely against database exfiltration and offline brute-force/rainbow table attacks.

### Decision
Use **BCrypt with adaptive work factor** via Spring Security's `BCryptPasswordEncoder` (default cost factor 10).

### Rationale
1. **Built-in Salt Generation:** BCrypt automatically generates and embeds a cryptographically secure 128-bit salt within the hash string, defending against pre-computed rainbow tables.
2. **Adaptive Cost Factor:** Work factor can be scaled up as compute power increases without altering existing schema.
3. **Industry Standard:** Broadly audited, battle-tested standard for authentication storage.

---

## ADR-004: Concurrency Strategy for Inventory Updates (Atomic Conditional Update)

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
When multiple library members attempt to borrow the last available copy of a popular book simultaneously, a race condition can allow `available_copies` to drop below zero, corrupting inventory data and violating database check constraints (`0 <= available_copies <= total_copies`).

We evaluated three potential locking mechanisms:
1. **Pessimistic Locking (`SELECT FOR UPDATE`):** Explicitly locks the book row from the start of the transaction until commit.
2. **Optimistic Locking with JPA `@Version`:** Detects version collisions at commit time and throws an `OptimisticLockException`, requiring application retry loops.
3. **Atomic Conditional Update:** Executes an atomic SQL query:
   ```sql
   UPDATE books
   SET available_copies = available_copies - 1, updated_at = NOW()
   WHERE id = :bookId AND available_copies > 0;
   ```

### Decision
Use **Atomic Conditional Update** (`WHERE available_copies > 0`) combined with database check constraints.

### Rationale
1. **Lock Duration Minimization:** Row-level locks in PostgreSQL are held only for the duration of the single `UPDATE` statement execution, rather than for the lifetime of the entire Spring `@Transactional` method.
2. **No Retry Overhead:** Unlike optimistic locking, there is no need for application-level retry loops or exponential backoff handling when competing for the last copy.
3. **Deterministic Row Count:** PostgreSQL returns the count of affected rows (`1` on success, `0` if another transaction claimed the copy first). If `0` rows are updated, the service immediately throws `ConflictException` (mapped to HTTP 409 Conflict), providing clean feedback.
4. **Defense in Depth:** The underlying PostgreSQL `CHECK (available_copies >= 0 AND available_copies <= total_copies)` constraint guarantees that even in extreme edge cases, data integrity cannot be violated at the database level.

### Proof & Verification
Verified with `LoanConcurrencyIntegrationTest`: two parallel threads simultaneously dispatch borrow requests for a book with 1 remaining copy using a `CountDownLatch`. Exactly one receives HTTP 201/success, exactly one receives HTTP 409 Conflict, and `available_copies` remains exactly `0`.

---

## ADR-005: Schema Management via Flyway (vs. Hibernate ddl-auto)

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
Database schemas can either be automatically generated by ORM tools (`hibernate.ddl-auto=update/create`) or maintained via version-controlled migration scripts.

### Decision
Use **Flyway** with strict SQL migration scripts and set `spring.jpa.hibernate.ddl-auto=validate`.

### Rationale
1. **Deterministic & Version-Controlled:** SQL migrations are committed to Git, reviewed in pull requests, and applied identically across dev, CI, staging, and production environments.
2. **Zero Surprises:** Prevents Hibernate from executing unexpected destructive `ALTER TABLE` or `DROP COLUMN` commands at application startup.
3. **Auditability:** Flyway maintains the `flyway_schema_history` table, recording exact timestamps, execution times, and checksums for all schema changes.

---

## ADR-006: Multi-Stage Dockerfile with Least-Privilege Execution

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
Containerized applications frequently suffer from bloated image sizes (including build tools, compilers, and local Maven repositories) and security vulnerabilities if run as root.

### Decision
Implement a **multi-stage build Dockerfile** deploying to an Alpine JRE runtime and executing under an unprivileged `spring:spring` system user.

### Rationale
1. **Minimal Attack Surface:** Build tools (JDK, Maven wrapper) are discarded after compilation; only the minimal Temurin JRE runtime and the executable JAR are packaged into the final image.
2. **Reduced Image Footprint:** Image size is kept under 250 MB.
3. **Least Privilege Principle:** Running as `USER spring:spring` prevents container breakout exploits from obtaining root privileges on the host kernel.

---

## ADR-007: Integration Testing with Testcontainers PostgreSQL (vs. H2 In-Memory)

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
Spring applications often test against in-memory H2 databases while running PostgreSQL in production. This creates subtle bugs due to divergent SQL dialects, missing PostgreSQL-specific check constraints, and differing transaction isolation and concurrency behaviors.

### Decision
Use **Testcontainers PostgreSQL** for all repository and integration test suites.

### Rationale
1. **Production Parity:** Tests execute against the exact same PostgreSQL container image (`postgres:17-alpine`) used in production and Docker Compose.
2. **Real Constraint Validation:** Database constraints such as `0 <= available_copies <= total_copies` and PostgreSQL row locking semantics behave identically in tests as in production.
3. **Zero Configuration for Developers:** Docker manages the ephemeral database lifecycle automatically; no local PostgreSQL installation is required.

---

## ADR-008: Cloud Database Connection String Normalization (Render / Heroku URI Support)

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Engineering Team

### Context
Cloud PaaS providers like Render, Heroku, and Railway inject managed database credentials as unified URI strings (e.g. `postgresql://user:password@host:port/database` or `postgres://...`). The official PostgreSQL JDBC driver (`pgjdbc`), however, requires the `jdbc:postgresql://` protocol prefix and explicitly rejects URI authority credentials (`user:password@`), throwing `java.lang.RuntimeException: Driver org.postgresql.Driver claims to not accept jdbcUrl`.

### Decision
Implement a dual-resilience strategy:
1. Configure Render Blueprint (`render.yaml`) to bind granular environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) via `fromDatabase`.
2. Implement a custom Spring Boot `EnvironmentPostProcessor` (`DatabaseUrlEnvironmentPostProcessor`) that intercepts unified cloud database URIs (`SPRING_DATASOURCE_URL`, `DATABASE_URL`, or `spring.datasource.url`), extracts credentials and parameters, and normalizes them into standard JDBC connection properties before connection pools initialize.

### Rationale
1. **Universal Portability:** The service boots seamlessly regardless of how the cloud provider injects database configuration—whether via discrete environment variables or a single unified URI.
2. **Early Lifecycle Execution:** Operating at the `EnvironmentPostProcessor` phase guarantees normalization before HikariCP or Flyway bootstrap, preventing driver handshake crashes.
3. **Zero Boilerplate for Operators:** Developers deploying to Render or similar cloud platforms require zero manual string manipulation or custom entrypoint shell scripts.

