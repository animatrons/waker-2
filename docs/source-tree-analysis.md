# Waker (waker-back) - Source Tree Analysis

**Date:** 2026-07-27

## Overview

`waker-back` is a single-module Maven project with a conventional `src/main/java` + `src/main/resources` + `src/main/webapp` layout. There is no `src/test` directory. The package root is `com.waker`, organized by architectural layer (`web`, `app`, `service`, `dao`, `model`, `util`) rather than by feature/domain — typical of the "layered" style used throughout.

`waker-front` exists as a sibling directory at the repository root but is **explicitly out of scope** and is not analyzed here or elsewhere in this documentation set; it will be replaced by a new native Android client.

## Complete Directory Structure

```
waker-2/                                   # Repository root
├── README.md                              # Setup instructions (env vars, mongo user, mvn build)
├── docs/                                  # Documentation (this set, ASSESSMENT_AND_ROADMAP.md, architecture PDFs)
├── waker-front/                           # OUT OF SCOPE (Ionic/Angular/Capacitor) — not documented
└── waker-back/                            # IN SCOPE — Java backend
    ├── pom.xml                            # Maven build, dependencies, dev/prod profiles (hardcoded Mongo creds)
    └── src/
        └── main/
            ├── java/com/waker/
            │   ├── Main.java              # Embedded Jetty bootstrap — application entry point
            │   ├── app/                   # Orchestration layer: validation, auth checks, cross-cutting flow
            │   │   ├── UserApp.java           # Register/login/token-validate orchestration
            │   │   ├── ReminderApp.java       # Reminder CRUD + ownership checks + penalty trigger orchestration
            │   │   └── PenaltyApp.java        # Thin wrapper around PenaltyFactory dispatch
            │   ├── web/                   # HTTP layer (Jakarta Servlets, annotation-registered)
            │   │   ├── ReminderServlet.java           # POST/GET /auth/api/reminder
            │   │   ├── ReminderFulfilledServlet.java  # POST /auth/api/reminder/fulfilled
            │   │   ├── ReminderMissedServlet.java     # POST /auth/api/reminder/missed (client-driven penalty trigger)
            │   │   ├── TestWithMeServlet.java         # POST /auth/api/test — dev-only, unauthenticated email test
            │   │   ├── auth/
            │   │   │   ├── LoginServlet.java          # POST /auth/api/login
            │   │   │   └── RegisterUserServlet.java   # POST /auth/api/registration
            │   │   └── filter/
            │   │       ├── Filter.java                # Global OPTIONS/CORS preflight handling, all paths
            │   │       └── AuthFilter.java             # JWT gate + CORS headers for /auth/*
            │   ├── service/               # Business/integration services
            │   │   ├── IUserService.java / IReminderService.java / IBaseService.java   # Service interfaces
            │   │   ├── IMailServiceProvider.java / ITemplatingService.java             # Integration interfaces
            │   │   ├── EmailService.java                                              # Console-stub "email" sender used by penalty services
            │   │   ├── impl/
            │   │   │   ├── BaseService.java              # Generic service base delegating to a DAO
            │   │   │   ├── UserService.java               # JWT build/validate, password hash/validate, user lookup
            │   │   │   ├── ReminderService.java           # Thin CRUD pass-through
            │   │   │   ├── GmailApiService.java           # Primary email sender (domain-delegated service account)
            │   │   │   ├── PostmarkMailService.java       # Wired but unused email sender
            │   │   │   ├── MailSlurpService.java          # Deprecated email sender (per its own javadoc)
            │   │   │   ├── SmtpMailService.java           # Unfinished/stub email sender
            │   │   │   └── HandlebarsTemplatingService.java  # Renders .hbs templates from classpath
            │   │   └── penalty/           # Strategy + Factory pattern for penalty types — the most extensible part of the codebase
            │   │       ├── IPenaltyService.java           # Strategy interface: penalize(T settings)
            │   │       ├── PenaltyFactory.java             # Simple factory: signature string → concrete IPenaltyService
            │   │       └── impl/
            │   │           ├── GetScoldedService.java
            │   │           ├── EmbarrassingTextService.java
            │   │           └── EmbarrassingEmailService.java
            │   ├── dao/                    # Data access layer (MongoDB via Jongo)
            │   │   ├── IGenericDao.java / AGenericDao.java   # Generic CRUD interface + reflection-based impl
            │   │   ├── IMongodbManager.java / MongodbManagerFactory.java  # Per-domain Mongo connection management
            │   │   ├── IUserDao.java / IReminderDao.java     # Marker interfaces extending IGenericDao<T>
            │   │   └── impl/
            │   │       ├── UserDao.java
            │   │       └── ReminderDao.java
            │   ├── model/                  # Domain entities, DTOs, and supporting types
            │   │   ├── AModel.java                     # Abstract Mongo document base (key/createdAt/updatedAt/code/label/enabled)
            │   │   ├── User.java                        # → collection "users"
            │   │   ├── Reminder.java                    # → collection "reminders" (the core "commitment" entity today)
            │   │   ├── Address.java                     # Embedded value object (collectionName = null)
            │   │   ├── Email.java                       # Plain POJO (from/to/subject/body/date), not a Mongo entity
            │   │   ├── FulfillmentMethod.java            # name + Map<Object,Object> setting, validated against Fulfillments enum
            │   │   ├── Fulfillments.java                 # Enum with a single value: WRITE_SOMETHING
            │   │   ├── penalty/
            │   │   │   ├── APenalty.java                 # Abstract, Jackson @JsonTypeInfo-polymorphic base
            │   │   │   ├── Penalties.java                 # Enum registry of penalty type names
            │   │   │   └── impl/
            │   │   │       ├── GetScolded.java
            │   │   │       ├── EmbarrassingText.java
            │   │   │       └── EmbarrassingEmail.java
            │   │   ├── dto/                              # Data transfer objects (Servlet ↔ App boundary)
            │   │   │   ├── ADto.java / UserDTO.java / UserOutputDTO.java / ReminderDTO.java / AddressDTO.java
            │   │   │   ├── MailDTO.java / ResponseDTO.java
            │   │   │   └── mapper/                       # MapStruct interfaces (impls generated at build time)
            │   │   │       ├── IMapper.java
            │   │   │       ├── ReminderMapper.java / UserMapper.java / UserOutputMapper.java
            │   │   ├── serialization/
            │   │   │   └── ReminderJsonAdapter.java       # Gson custom deserializer resolving APenalty subtype by "_class"
            │   │   └── exception/
            │   │       ├── GeneralException.java / BusinessException.java / TechnicalException.java
            │   │       └── BusinessErrorCodesAndMessages.java / TechnicalErrorCodesAndMessages.java
            │   └── util/
            │       ├── ConfigProperties.java              # Loads config.properties (Maven-filtered)
            │       ├── Tools.java                         # Base64/hex helpers, JWT header decode, constant-time compare
            │       └── security/
            │           └── Crypt.java                     # HMAC-SHA256 signing + PBKDF2WithHmacSHA1 password hashing
            ├── resources/
            │   ├── config.properties                       # Maven-filtered: mongodb user/pwd/db name, iso.date.format
            │   └── email_templates/
            │       ├── user_registration_confirmation_email.hbs
            │       └── simple_basic_template.html
            └── webapp/WEB-INF/
                ├── web.xml                                 # Minimal Servlet 3.1 descriptor (welcome file, 404 error page)
                └── loggin.properties                       # (filename typo, as in source) logging config
```

## Critical Directories

### `src/main/java/com/waker/web/`

**Purpose:** HTTP entry points (Jakarta `@WebServlet`/`@WebFilter`, annotation-registered — no `web.xml` servlet mappings).
**Contains:** 6 servlets + 2 filters.
**Entry Points:** All servlets are effectively entry points; `AuthFilter` gates everything under `/auth/*` except a small whitelist.

### `src/main/java/com/waker/app/`

**Purpose:** Orchestration layer between HTTP and services — validation, ownership checks, and multi-service coordination that doesn't belong in either a servlet or a single service.
**Contains:** `UserApp`, `ReminderApp`, `PenaltyApp` — each a hand-written singleton (`getInstance()`), not a DI-managed bean.

### `src/main/java/com/waker/service/penalty/`

**Purpose:** The one deliberately extensible subsystem in the codebase — isolates per-penalty-type business logic behind a common `IPenaltyService<T>` interface, resolved via `PenaltyFactory`.
**Contains:** 1 interface, 1 factory, 3 concrete strategy implementations (all console-log stubs currently).
**Integration:** Invoked exclusively from `PenaltyApp.punish(APenalty)`, itself only reachable via `ReminderApp.takeAction(...)`, itself only reachable from `ReminderMissedServlet` — i.e., **client-triggered**, not scheduler-triggered.

### `src/main/java/com/waker/dao/`

**Purpose:** MongoDB access via the legacy `mongo-java-driver` + Jongo ODM.
**Contains:** A generic, reflection-based CRUD implementation (`AGenericDao`) shared by `UserDao` and `ReminderDao`; a per-domain-name Mongo connection factory (`MongodbManagerFactory`).

### `src/main/java/com/waker/model/`

**Purpose:** Domain entities (Mongo documents), DTOs, and the polymorphic penalty type hierarchy.
**Contains:** See `data-models.md` for the full entity breakdown.

### `src/main/resources/email_templates/`

**Purpose:** Handlebars/HTML templates for outbound email.
**Contains:** Exactly one wired template (`user_registration_confirmation_email.hbs`) and one unused static HTML template (`simple_basic_template.html`).

## Entry Points

- **Main Entry:** `com.waker.Main` — starts an embedded Jetty `Server`, builds a `WebAppContext` pointed at `src/main/webapp/WEB-INF/web.xml`, and relies on annotation scanning to discover `@WebServlet`/`@WebFilter` classes on the classpath. Reads the `PORT` environment variable (throws if unset).
- **Additional (per-route) entry points:** each `@WebServlet`-annotated class in `web/` and `web/auth/` is independently reachable once the server is running; see `api-contracts.md` for the full route table.

## File Organization Patterns

- **Layer-first, not feature-first**: packages are named after architectural role (`web`, `app`, `service`, `dao`, `model`) rather than business domain (`user`, `reminder`, `penalty`) — the domain concept only shows up as a naming prefix inside each layer (e.g. `UserService`, `ReminderService`) or as a subpackage for the one polymorphic type family (`service/penalty`, `model/penalty`).
- **Interface + impl pairing**: nearly every service/DAO has an `I*` interface in the parent package and a concrete class in a nested `impl/` package (e.g. `service/IUserService.java` + `service/impl/UserService.java`).
- **Singleton-via-static-getInstance()**: every service, DAO, app, and factory class is a hand-rolled singleton; there is no dependency-injection container anywhere in the codebase.

## Key File Types

### Servlets

- **Pattern:** `web/**/*Servlet.java`
- **Purpose:** HTTP request handling, one class per route (or closely related route group)
- **Examples:** `ReminderServlet`, `LoginServlet`, `RegisterUserServlet`

### App orchestrators

- **Pattern:** `app/*App.java`
- **Purpose:** Cross-cutting orchestration between HTTP and service layers (validation, authorization, multi-service coordination)
- **Examples:** `UserApp`, `ReminderApp`, `PenaltyApp`

### Service interfaces/implementations

- **Pattern:** `service/I*.java` (interface), `service/impl/*.java` (implementation)
- **Purpose:** Business logic and third-party integration (email, templating, JWT)
- **Examples:** `IUserService`/`UserService`, `IMailServiceProvider`/`GmailApiService`

### DAOs

- **Pattern:** `dao/I*Dao.java`, `dao/AGenericDao.java`, `dao/impl/*Dao.java`
- **Purpose:** MongoDB persistence via Jongo
- **Examples:** `IReminderDao`/`ReminderDao`

### Domain models / DTOs

- **Pattern:** `model/*.java`, `model/dto/*.java`
- **Purpose:** Mongo document entities and their HTTP-facing DTO counterparts
- **Examples:** `Reminder`/`ReminderDTO`, `User`/`UserDTO`

## Asset Locations

No significant binary/media assets detected. The only non-code resources are two email templates under `src/main/resources/email_templates/` and a Maven-filtered `config.properties`.

## Configuration Files

- **`waker-back/pom.xml`**: Maven build, dependency versions, `dev`/`prod` profiles — currently contains hardcoded MongoDB credentials (`wakerman` / `1111`) in both profiles.
- **`src/main/resources/config.properties`**: Maven-filtered at build time from the active profile's `mongodb.*` properties plus `iso.date.format`.
- **`src/main/webapp/WEB-INF/web.xml`**: Minimal Servlet 3.1 descriptor — welcome file and a 404 error page mapping only; no servlet/filter declarations (those are annotation-driven).
- **Environment variables** (read directly via `System.getenv(...)`, not centralized): `PORT`, `mongodbHost`, `mongodbPort`, `MAIN_EMAIL_ADDRESS`, `SECONDARY_EMAIL_ADDRESS`, `POSTMARK_API_TOKEN`, `MAIL_SLURP_API_KEY`, `MAIL_SLURP_DEFAULT_INBOX_ID`, `DEFAULT_INBOX_EMAIL`.

## Notes for Development

- There is no `src/test` directory — any new work should establish a test setup (JUnit5 + Mockito at minimum) alongside whatever else is built, per the roadmap's Phase 1.
- The `AuthFilter` whitelist (`/auth/api/registration`, `/auth/api/login`, `/auth/api/test`) includes the dev-only test-email endpoint — this should not ship as-is.
- `SmtpMailService` is a non-functional stub (empty credential constants, `send()` returns `null` on the success path and is unreachable in practice); do not build on top of it without finishing or replacing it.
- Several classes reference `org.apache.commons.lang3.StringUtils` and `org.apache.commons.codec.binary.Base64` without an explicit `commons-lang3`/`commons-codec` dependency declared in `pom.xml` — they currently resolve transitively (via MailSlurp/Google client libraries); this is fragile and should be made explicit if those dependencies are ever removed.

---

_Generated using the BMAD Method `document-project` workflow_
