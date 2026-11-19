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
