# Waker Documentation Index

**Type:** Monolith (single in-scope part: `waker-back`)
**Primary Language:** Java 17
**Architecture:** Layered (Servlet → App → Service → DAO), embedded Jetty, MongoDB/Jongo, no DI framework
**Last Updated:** 2026-07-27

## Project Overview

Waker is a productivity/alarm app that penalizes users automatically when they fail to prove they completed a committed task within a deadline. This repository's `waker-back` directory contains a ~4-year-old Java backend implementing an early, partial slice of that idea (registration/login, a `Reminder` entity, 3 trivial penalty types, no scheduler, no missions). `waker-front` (Ionic/Angular hybrid) is present in the repository but is **explicitly excluded from scope** — it is being replaced by a new native Android client, per project decision. All documentation, analysis, and forward planning in this `docs/` folder therefore treats `waker-back` as the only in-scope part of the repository.

## Quick Reference

- **Tech Stack:** Java 17, embedded Jetty 11 (Jakarta Servlets), MongoDB 3.x + Jongo, Gson (custom polymorphic penalty adapter), MapStruct + Lombok, hand-rolled HMAC-SHA256 JWT + PBKDF2 password hashing, Handlebars (1 email template), Gmail API for outbound mail
- **Entry Point:** `com.waker.Main` (embedded Jetty bootstrap)
- **Architecture Pattern:** Layered / transaction-script, with a Strategy+Factory pattern isolating penalty-type logic (`APenalty` / `IPenaltyService` / `PenaltyFactory`)
- **Database:** MongoDB (collections: `users`, `reminders`) — planned migration to PostgreSQL + JSONB
- **Deployment:** None currently (no Docker, no CI/CD) — planned: multi-stage Docker build + `docker-compose.yml` with PostgreSQL

## Generated Documentation

### Core Documentation (current, as-built state)

- [Project Overview](./project-overview.md) - Executive summary and high-level architecture
- [Source Tree Analysis](./source-tree-analysis.md) - Fully annotated directory structure (every file in `waker-back/src/main` was read to produce this)
- [Architecture](./architecture.md) - Detailed technical architecture, layering, extension points, and known architectural issues
- [Development Guide](./development-guide.md) - Local setup, environment variables, build/run/test workflow
- [API Contracts](./api-contracts.md) - All 7 REST endpoints, request/response shapes, auth mechanism, and documented behavioral quirks/bugs
- [Data Models](./data-models.md) - MongoDB/Jongo entity model (`User`, `Reminder`, `APenalty` hierarchy) and planned PostgreSQL target schema
- [Deployment Guide](./deployment-guide.md) - Current state (none) and the planned Docker/Postgres-based deployment approach

### Planning Documentation (target state and roadmap)

- [Assessment & Roadmap](./ASSESSMENT_AND_ROADMAP.md) - Full technical assessment, business-rule feasibility analysis (24h commitment window, no-cancellation, penalty limits), per-penalty and per-mission implementation notes, payment-penalty strategy (Merchant-of-Record for subscriptions + personal PayPal Payouts for dogfooding "pay $" penalty), and the phased delivery plan from Phase 0 through general availability

### Existing Documentation

- [README.md](../README.md) - Original setup instructions (front-end Ionic/Capacitor setup is now obsolete per the native-Android decision; backend env-var list has one known typo, documented in `development-guide.md`)
- [Backend Architecture Diagram (PDF)](./waker_app_back.drawio.pdf) - Original hand-drawn architecture diagram (generic DAO/Service abstraction)
- [Penalty System Diagram (PDF)](./waker_app_penal_system.drawio.pdf) - Original hand-drawn penalty Strategy/Factory diagram

## Getting Started

### Prerequisites

JDK 17, Maven, a running MongoDB instance (see `development-guide.md` for exact setup commands and required environment variables).

### Setup

```bash
cd waker-back
mvn clean install
```

### Run Locally

```bash
PORT=8888 mongodbHost=localhost mongodbPort=27017 \
SECONDARY_EMAIL_ADDRESS=<your-workspace-domain-email> \
java -cp target/classes:target/dependency/* com.waker.Main
```

### Run Tests

No test suite exists yet (`mvn test` runs against an empty `src/test`). See `development-guide.md` and the roadmap for the plan to introduce JUnit5/Mockito/Testcontainers.

## For AI-Assisted Development

This documentation was generated specifically to enable AI agents to understand and extend this codebase, and to support planning the project's next phases (see the roadmap).

### When Planning New Features:

**Backend features on the current codebase:**
→ Reference: `architecture.md`, `api-contracts.md`, `data-models.md`

**Features described in the product vision but not yet built (missions, real penalties, payments, scheduler):**
→ Reference: `ASSESSMENT_AND_ROADMAP.md` first (it documents feasibility, rules, and the target design for each), then `architecture.md`'s "Planned Evolution" section for how they fit the target architecture

**Native Android client work:**
→ `waker-front` is deprecated/out of scope; there is no existing native client code to reference yet — this will be new, greenfield work per the roadmap

**Deployment changes:**
→ Reference: `deployment-guide.md` (documents both current absence of deployment config and the planned Docker/Postgres approach)

---

_Documentation generated by the BMAD Method `document-project` workflow_
