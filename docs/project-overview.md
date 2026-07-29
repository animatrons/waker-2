# Waker (waker-back) - Project Overview

**Date:** 2026-07-27
**Type:** backend (Java / Maven, embedded servlet application)
**Architecture:** Layered monolith with a custom orchestration ("App") layer, manual singleton DI, and a Strategy+Factory extension point for penalty types

## Executive Summary

Waker is a productivity/alarm app built around a penalty mechanic: a user commits to a task (e.g. "wake up at 6am"), must prove they fulfilled it within a configured window, and is automatically penalized if they don't. The codebase in this repository (`waker-back`) is a ~4-year-old, largely dormant Java backend that implements an early, partial slice of this idea under a different vocabulary (`Reminder` instead of `Commitment`, no `Mission` abstraction yet, only 3 trivial `Penalty` types). It was last substantively touched around August 2022.

**Scope note:** `waker-front` (the existing Ionic/Angular/Capacitor hybrid mobile client) is explicitly **out of scope** for this documentation set and for future development. The project is moving to a **native Android** client, so this documentation, and all forward planning, treats `waker-back` as the sole in-scope part of this repository.

This documentation set describes the backend **as it exists today** (for AI context / brownfield planning purposes). For the agreed **target architecture and phased delivery plan** (Spring Boot migration, PostgreSQL, native Android, Mission abstraction, payment-penalty strategy, etc.), see [`ASSESSMENT_AND_ROADMAP.md`](./ASSESSMENT_AND_ROADMAP.md) — that document is the source of truth for where the project is headed; this one is the source of truth for where it is now.

## Project Classification

- **Repository Type:** Monolith (single in-scope part: `waker-back`; `waker-front` present in the repo but explicitly excluded/deprecated)
- **Project Type(s):** backend
- **Primary Language(s):** Java 17
- **Architecture Pattern:** Layered (Servlet → App → Service → DAO) with manual singleton dependency management (no DI framework), plus a Strategy/Factory pattern isolating penalty-type-specific logic

## Technology Stack Summary

| Category | Technology | Version | Notes |
|---|---|---|---|
| Language | Java | 17 (`maven.compiler.source/target`) | |
| Build | Maven | - | `pom.xml`, no wrapper committed |
| Web runtime | Eclipse Jetty (embedded) | 11.0.9 | Not Spring Boot; hand-bootstrapped `WebAppContext` in `Main.java` |
| Servlet API | Jakarta Servlet (annotation-driven) | via Jetty 11 | `@WebServlet` / `@WebFilter`, minimal `web.xml` |
| Database driver | `mongo-java-driver` | 3.12.11 | Legacy synchronous driver (deprecated upstream) |
| ODM | Jongo | 1.5.0 | Unmaintained since ~2016; Jackson-based Mongo mapping |
| JSON | Gson | 2.9.0 | Custom `JsonDeserializer` for polymorphic penalty payloads |
| Object mapping | MapStruct | 1.5.2.Final | Entity ↔ DTO mapping, generated impls |
| Boilerplate | Lombok | 1.18.24 | Getters/setters/constructors |
| Logging | SLF4J + slf4j-simple | 1.7.36 | Console logging only |
| Email (primary) | Gmail API (`google-api-client`, `google-oauth-client-jetty`, `google-api-services-gmail`) | 2.0.0 / 1.34.1 / v1-rev20220404-2.0.0 | Domain-delegated service account |
| Email (wired, unused) | Postmark Java SDK | 1.8.0 | Implemented, not selected in `UserApp` |
| Email (deprecated) | MailSlurp client | 15.13.1 | Marked "Useless and deprecated" in its own class javadoc |
| Email (stub) | `javax.mail` (JavaMail) | 1.5.0-b01 | `SmtpMailService.send()` is unfinished, returns `null` |
| Templating | Handlebars (`com.github.jknack`) | 4.3.0 | One template: registration confirmation email |
| Auth | Hand-rolled HMAC-SHA256 JWT + PBKDF2WithHmacSHA1 password hashing | - | No Spring Security / no third-party auth library |
| Tests | None | - | No `src/test` directory exists |
| Containerization | None | - | No `Dockerfile` / `docker-compose.yml` |

## Key Features (as implemented today)

- User registration and login with a custom JWT (`/auth/api/registration`, `/auth/api/login`)
- Create/read a `Reminder` (name, description, notify time, deadline, fulfillment method, penalty setting) — `/auth/api/reminder`
- Mark a reminder fulfilled — `/auth/api/reminder/fulfilled`
- Mark a reminder missed, which **synchronously triggers** the configured penalty — `/auth/api/reminder/missed` (client-driven; see Architecture Highlights)
- Three trivial penalty types, dispatched via a factory: `GetScolded`, `EmbarrassingText`, `EmbarrassingEmail` — all currently console-log stubs except the email path
- One fulfillment type placeholder (`WRITE_SOMETHING`) with no validation logic behind it
- A test-only email-sending endpoint (`/auth/api/test`) that is (problematically) on the unauthenticated route whitelist

## Architecture Highlights

- **Not Spring.** Everything is hand-wired: `Main.java` boots an embedded Jetty `Server`/`WebAppContext` that annotation-scans for `@WebServlet`/`@WebFilter` classes; there is no dependency-injection container.
- **Extra "App" orchestration layer**: `UserApp` / `ReminderApp` / `PenaltyApp` sit between the servlets and the services, doing DTO validation, ownership checks (`isOperationAllowed`), and cross-cutting orchestration (e.g. `ReminderApp.takeAction` calling into `PenaltyApp`).
- **Generic DAO via reflection**: `AGenericDao<T extends AModel>` re-implements generic CRUD (find/save/delete/count) against Jongo using `targetClass.getDeclaredConstructor().newInstance()` to work around Java type erasure — the kind of plumbing a framework like Spring Data eliminates.
- **The one genuinely extensible piece**: penalties are modeled as `APenalty` (abstract, Jackson `@JsonTypeInfo`-polymorphic) → `GetScolded` / `EmbarrassingText` / `EmbarrassingEmail`, dispatched to a matching `IPenaltyService<T>` via `PenaltyFactory.getService(signature)` (a hand-written `if/else` chain with an unchecked cast). This Strategy+Factory shape is the part of the design worth preserving conceptually when the project moves to a proper framework.
- **No autonomous enforcement.** There is no scheduler, cron job, or background worker anywhere in the codebase. A penalty only fires because a client explicitly calls `POST /auth/api/reminder/missed`. This is the most significant architectural gap relative to the product's core promise (see `ASSESSMENT_AND_ROADMAP.md` §1 and §3.1).
- **No Mission abstraction exists yet.** `Fulfillments` is a one-value enum (`WRITE_SOMETHING`); there are no QR/location/math/speech mission types in code, only in the product vision.

## Development Overview

### Prerequisites

- JDK 17
- Maven
- A running MongoDB instance (a database named `wakerdb`, with a user `wakerman`/`1111` — credentials are hardcoded in `pom.xml` Maven profiles today)
- Environment variables: `PORT`, `mongodbHost`, `mongodbPort`, `MAIN_EMAIL_ADDRESS`, `SECONDARY_EMAIL_ADDRESS`, `POSTMARK_API_TOKEN` (README calls this `POSTMART_API_KEY` — a typo relative to the code)

### Getting Started

```bash
# from waker-back/
mvn clean install
# then run com.waker.Main with the required env vars set, e.g.:
# PORT=8888 mongodbHost=localhost mongodbPort=27017 java -cp <classpath> com.waker.Main
```

There is currently no Docker setup, so a local JVM + local/remote MongoDB instance is required to run the app. Planned: Docker multi-stage build + `docker-compose.yml` (see `ASSESSMENT_AND_ROADMAP.md` §3.3 and Phase 1).

### Key Commands

- **Install/Build:** `mvn clean install`
- **Run:** run `com.waker.Main` (reads `PORT` env var; no `mvn exec` plugin is currently wired — the commented-out `exec-maven-plugin` block in `pom.xml` shows a prior attempt)
- **Test:** none exist (`mvn test` runs against an empty `src/test`)

## Repository Structure

```
waker-2/
├── README.md
├── docs/                          # Architecture PDFs, this documentation set, ASSESSMENT_AND_ROADMAP.md
├── waker-back/                    # IN SCOPE — Java backend documented here
│   └── src/main/{java,resources,webapp}/...
└── waker-front/                   # OUT OF SCOPE — Ionic/Angular hybrid client, to be replaced by native Android
```

## Documentation Map

For detailed information, see:

- [index.md](./index.md) - Master documentation index
- [architecture.md](./architecture.md) - Detailed technical architecture (current state)
- [source-tree-analysis.md](./source-tree-analysis.md) - Annotated directory structure
- [development-guide.md](./development-guide.md) - Local setup and development workflow
- [api-contracts.md](./api-contracts.md) - Current REST endpoints
- [data-models.md](./data-models.md) - Current MongoDB/Jongo entity model
- [deployment-guide.md](./deployment-guide.md) - Current (none) and planned deployment setup
- [ASSESSMENT_AND_ROADMAP.md](./ASSESSMENT_AND_ROADMAP.md) - Target architecture, business-rule feasibility analysis, and phased delivery roadmap

---

_Generated using the BMAD Method `document-project` workflow_
