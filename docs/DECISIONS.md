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
