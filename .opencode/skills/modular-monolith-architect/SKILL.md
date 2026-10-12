---
name: modular-monolith-architect
description: Enforce Modular Monolith architectural principles in Spring Boot and full-stack applications. Use when auditing, planning, reviewing, or writing backend code to guarantee domain encapsulation, lookup facade patterns, package-by-feature isolation, and zero cross-repository dependencies.
---

# Modular Monolith Architectural Principles & Verification

Guarantees strict compliance with Modular Monolith standards for Spring Boot and full-stack applications.

## Core Rules

- **Package Isolation (Package-by-Feature):**
  - Organize domain code vertically: `modules/users`, `modules/services`, `modules/reservations`, `modules/core`.
  - Avoid horizontal layering (e.g., `com.app.controllers`, `com.app.services`).

- **Zero Cross-Module Repository Injections:**
  - A service in Module A MUST NEVER inject a `@Repository` from Module B.
  - Example: `ReservaServiceImpl` must not inject `UserRepository` or `ServiceRepository`.

- **Inter-Module Communication via Facades & DTOs:**
  - Modules communicate exclusively through published Lookup Services / Facades (e.g., `ClientLookupService`, `ServiceLookupService`).
  - Facades MUST return lightweight read-only records/DTOs (e.g., `ClientDisplayInfo`), NEVER JPA entities (preventing entity leaks across domain boundaries).

- **Acyclic Package & Bean Graph:**
  - Maintain clean one-way dependencies between modules.
  - Avoid bidirectional package imports between feature modules.

- **Cross-Cutting Core Isolation:**
  - `modules/core` handles security, JWT, global exception handling, and framework configs only.
  - Core MUST NOT own domain entities, repositories, or business rules.

## Verification Checklist

When building or reviewing any feature:

- [ ] No cross-module repository injections present.
- [ ] Facade calls return read-only DTOs/Records instead of JPA entities.
- [ ] `@Transactional(readOnly = true)` applied on read operations; `@Transactional` on mutations.
- [ ] Lazy initialization proxies handled cleanly inside active transactions.
