# Waker (waker-back) - Architecture

**Date:** 2026-07-27
**Scope:** `waker-back` only. `waker-front` is out of scope (see [project-overview.md](./project-overview.md)).
**Status:** Documents the **current, as-built** architecture for brownfield/AI context. For the **target** architecture (Spring Boot, PostgreSQL, scheduler, Mission abstraction, native Android client, payment strategy), see [ASSESSMENT_AND_ROADMAP.md](./ASSESSMENT_AND_ROADMAP.md).

## Executive Summary

`waker-back` is a hand-built, framework-light Java 17 web backend: an embedded Jetty server hosting annotation-registered Jakarta servlets, backed by MongoDB via the unmaintained Jongo ODM. There is no dependency-injection framework — every service/DAO/orchestrator is a manual singleton. The design deliberately isolates one axis of variability (penalty types) behind a Strategy+Factory pair, but does not yet do the same for missions/fulfillment methods, and has no background scheduling of any kind, meaning penalty enforcement today only happens if a client explicitly asks for it.

## Technology Stack

See [project-overview.md](./project-overview.md#technology-stack-summary) for the full table. Summary: Java 17, embedded Jetty 11 (not Spring Boot), MongoDB 3.x driver + Jongo, Gson (custom polymorphic adapter), MapStruct + Lombok, hand-rolled JWT/PBKDF2, Handlebars for one email template, no tests, no containerization.

## Architecture Pattern

**Layered / "transaction script" architecture with an extra orchestration tier:**

```
HTTP request
   │
   ▼
Filter chain            (Filter → AuthFilter, both global path-scoped @WebFilter)
   │
   ▼
Servlet                 (@WebServlet, one per route — thin: JSON (de)serialize + delegate)
   │
   ▼
"App" orchestrator       (UserApp / ReminderApp / PenaltyApp — validation, ownership checks,
   │                       cross-service coordination; NOT a Spring/CDI-managed bean)
   ▼
Service                 (IUserService/UserService, IReminderService/ReminderService,
   │                      IPenaltyService/*Service — business logic + integrations)
   ▼
DAO                     (AGenericDao<T> → Jongo → MongoDB)
```

Every layer boundary is crossed via a hand-written singleton (`private static X instance; getInstance()`), not a DI container. This is functionally equivalent to Spring's singleton-scoped beans, but requires manual wiring and offers no lifecycle hooks, no proxying (so no `@Transactional`-style cross-cutting concerns), and no easy test substitution (mocking requires reflection or refactoring toward interfaces at call sites, which is only partially done today).

### The one deliberate extension point: penalties

```
APenalty (abstract, Jackson @JsonTypeInfo polymorphic on "_class")
  ├── GetScolded          → GetScoldedService          implements IPenaltyService<GetScolded>
  ├── EmbarrassingText    → EmbarrassingTextService     implements IPenaltyService<EmbarrassingText>
  └── EmbarrassingEmail   → EmbarrassingEmailService    implements IPenaltyService<EmbarrassingEmail>

PenaltyFactory.getInstance().getService(signature) → if/else chain matching Penalties enum values,
                                                       returns an unchecked-cast IPenaltyService<APenalty>
```

This Strategy (`IPenaltyService<T>`) + Simple Factory (`PenaltyFactory`) pairing is the part of the original design that is worth **conceptually preserving** — it is exactly the shape needed to keep adding penalty types (and, by extension, mission/fulfillment types, which currently have no equivalent abstraction) without touching core reminder-processing code. Its current implementation issues (unchecked casts, `if/else` instead of a `Map<String, Supplier<IPenaltyService<?>>>`, no dependency injection) are mechanical and can be modernized without discarding the pattern.

**No equivalent exists for fulfillment/mission types.** `Fulfillments` is a bare enum with one value (`WRITE_SOMETHING`) and `FulfillmentMethod` only validates that its `name` field matches an enum constant — there is no `IFulfillmentService`/`FulfillmentFactory` counterpart, and no code path that actually verifies a submitted fulfillment (e.g. checks a QR payload, a GPS coordinate, a math answer). This has to be designed from scratch for the mission types described in the product roadmap.

## Data Architecture

See [data-models.md](./data-models.md) for the full entity/collection breakdown. In short: two MongoDB collections (`users`, `reminders`) accessed through Jongo, with `Reminder.penaltySetting` embedding a polymorphic `APenalty` document distinguished by a Jackson `_class` discriminator field.

### Generic DAO via reflection

`AGenericDao<T extends AModel>` implements `IGenericDao<T>` once, generically, for all entities:

```java
// com.waker.dao.AGenericDao (abridged)
public abstract class AGenericDao<T extends AModel> implements IGenericDao<T> {
    protected Class<T> targetClass;
    protected AGenericDao(Class<T> targetClass) { this.targetClass = targetClass; }

    public T find(String id) throws TechnicalException {
        MongoCollection collection = getCollection();
        return collection.findOne(new ObjectId(id)).as(targetClass);
    }
    // find(query, params, projection, sort, pageSize, start), delete(...), addOrUpdate(...), count(...)
    // all implemented once here, reused by UserDao and ReminderDao via inheritance
}
```

This is the exact problem Spring Data repositories solve out of the box (derive CRUD + query methods from an interface, no reflection-based base class needed) — see the roadmap's recommendation to replace this layer wholesale rather than modernize it in place.

## API Design

See [api-contracts.md](./api-contracts.md) for the full endpoint table with request/response shapes. Summary: 7 servlet-backed endpoints under `/auth/api/*`, gated by a custom JWT `AuthFilter` with a hardcoded route whitelist for registration/login/(dev-only test).

## Component Overview

| Component | Role | Extensibility today |
|---|---|---|
| `web/*Servlet` | HTTP I/O, JSON (de)serialization | Low — one class per route, adding a route means adding a class + no central route registry |
| `app/*App` | Validation, authorization, orchestration | Low — logic is hand-written per method, no cross-cutting mechanism (no interceptors/aspects) |
| `service/*` | Business logic, integrations (email, JWT) | Mixed — email providers are swappable behind `IMailServiceProvider`, but `UserApp` hardcodes which implementation it uses (`GmailApiService`) rather than injecting a choice |
| `service/penalty/*` | Penalty strategy dispatch | **Highest** — this is the one part of the codebase built for extension |
| `dao/*` | MongoDB persistence | Low — generic base class works, but is reflection-heavy boilerplate that a framework replaces declaratively |
| `model/*`, `model/dto/*` | Entities, DTOs, mapping | Medium — MapStruct mapping is reasonable; validation is manual boolean-returning methods scattered per DTO |

## Source Tree

See [source-tree-analysis.md](./source-tree-analysis.md) for the fully annotated directory tree.

## Development Workflow

See [development-guide.md](./development-guide.md).

## Deployment Architecture

No deployment configuration exists in the repository today (no `Dockerfile`, no CI/CD pipeline, no IaC). See [deployment-guide.md](./deployment-guide.md) for the current manual process and the planned Docker-based setup.

## Testing Strategy

**None exists.** There is no `src/test` directory, no JUnit/Mockito/Testcontainers dependency in `pom.xml`, and no CI configuration. This is a blocking gap for any refactor of the DAO/service layers — establishing a baseline test harness should happen before or alongside the Spring Boot migration described in the roadmap (Testcontainers against a real Postgres/Mongo instance is recommended there).

## Known Architectural Issues (current state)

These are documented here as *facts about the existing code* (for AI/brownfield context); see `ASSESSMENT_AND_ROADMAP.md` for the remediation plan and prioritization.

1. **No scheduler / no autonomous enforcement** — `ReminderMissedServlet` is the only path that triggers a penalty, and it must be called by a client. A user who simply doesn't open the app after missing a deadline is never penalized today. This is the single most important functional gap relative to the product's premise.
2. **Hardcoded secrets** — the JWT signing key is a hardcoded byte array in `UserService`; MongoDB credentials (`wakerman`/`1111`) are hardcoded in `pom.xml`'s `dev` and `prod` Maven profiles (i.e., identical, non-externalized credentials for both environments).
3. **Open CORS** — `AuthFilter` sets `Access-Control-Allow-Origin: *` unconditionally.
4. **Unauthenticated email-send endpoint** — `/auth/api/test` (`TestWithMeServlet`) is on `AuthFilter`'s whitelist and triggers a real `GmailApiService.send(...)` call with attacker-supplied `MailDTO` content — effectively an open relay through the app's Gmail service account if exposed.
5. **Multiple parallel, mostly-dead email integrations** — 4 `IMailServiceProvider` implementations exist (Gmail, Postmark, MailSlurp, SMTP); only Gmail is actually wired into `UserApp`; MailSlurp is explicitly marked deprecated in its own javadoc; SMTP is an unfinished stub that always returns `null`.
6. **Legacy/unmaintained data-layer dependencies** — `mongo-java-driver` 3.12.11 and Jongo 1.5.0 are both years past active maintenance; a commented-out `mongodb-driver-sync` 4.6.0 dependency in `pom.xml` shows an abandoned migration attempt.
7. **No input validation framework** — validation is ad hoc, per-DTO boolean methods (`UserDTO.validate()`, `ReminderDTO.validate()`) rather than declarative (e.g. Bean Validation).
8. **No fulfillment/mission abstraction** — `Fulfillments` enum has exactly one member and no service layer verifies fulfillment content; this entire subsystem needs to be designed and built, not migrated.

## Planned Evolution (summary — see ASSESSMENT_AND_ROADMAP.md for detail)

- Migrate to **Spring Boot 3.x on Java 21**, replacing hand-rolled singletons with Spring DI, `AGenericDao` with Spring Data repositories, and the manual JWT filter with Spring Security.
- Migrate persistence from **MongoDB/Jongo to PostgreSQL** (with JSONB for the polymorphic penalty/mission payloads), via Flyway/Liquibase migrations.
- Introduce a **`Commitment`/`Mission`/`Penalty` domain model** replacing `Reminder`/`Fulfillments`/`APenalty`, with a `IMissionService`/`MissionFactory` pair mirroring the existing penalty Strategy+Factory shape.
- Add a **Spring `@Scheduled`-based enforcement job** so penalties fire autonomously instead of requiring a client call.
- Containerize with **Docker** (multi-stage build) + `docker-compose.yml` for local Postgres + app.
- Replace the native Android client scope decision (Kotlin + Jetpack Compose) in place of `waker-front`.
- Introduce **Testcontainers + JUnit5 + Mockito** as the test baseline.
- Layered **payment-penalty strategy**: a Merchant-of-Record (Paddle/Lemon Squeezy) for Pro subscriptions, and a separate, initially personal-only, PayPal-Payouts-based automated "pay $" penalty for dogfooding, ahead of a general multi-user payment-processor integration.

---

_Generated using the BMAD Method `document-project` workflow_
