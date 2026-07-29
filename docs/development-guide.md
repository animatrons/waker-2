# Waker (waker-back) - Development Guide

**Date:** 2026-07-27

## Prerequisites

- **JDK 17** (matches `maven.compiler.source`/`target` in `pom.xml`)
- **Maven** (no wrapper committed to the repo — install locally, e.g. via sdkman or your OS package manager)
- **MongoDB** running and reachable (local instance or remote), with:
  - Database: `wakerdb`
  - User: `wakerman` / password: `1111` (hardcoded today in `pom.xml`'s `dev`/`prod` profiles — see Known Issues below)
- A Google Cloud service account with domain-wide delegation configured (only required if you need the registration email flow to actually send mail — see Email Setup below)

## Environment Variables

Set these before running `com.waker.Main` (e.g. in your IDE's run configuration, or exported in your shell):

| Variable | Required | Purpose |
|---|---|---|
| `PORT` | **Yes** — `Main.main()` throws immediately if unset (`Integer.parseInt(System.getenv("PORT"))`) | HTTP port for the embedded Jetty server |
| `mongodbHost` | Yes (for any DB-touching request) | Mongo host, read in `MongodbManagerFactory` |
| `mongodbPort` | Yes | Mongo port |
| `MAIN_EMAIL_ADDRESS` | Only for `PostmarkMailService` (currently unused in practice) | Sender address if Postmark were wired in |
| `SECONDARY_EMAIL_ADDRESS` | Yes, for registration emails to actually send | The Google Workspace domain email with domain-wide delegation enabled for the service account (`GmailApiService.WORKSPACE_DOMAIN_EMAIL`) |
| `POSTMARK_API_TOKEN` | Only for Postmark (unused) | **Note:** the README documents this as `POSTMART_API_KEY` (typo) — the actual code reads `POSTMARK_API_TOKEN` |
| `MAIL_SLURP_API_KEY`, `MAIL_SLURP_DEFAULT_INBOX_ID`, `DEFAULT_INBOX_EMAIL` | Only for `MailSlurpService` (deprecated, unused) | Not needed for normal operation |

`mongodb.user.name` / `mongodb.user.pwd` / `mongodb.db.name` are **not** environment variables — they come from `src/main/resources/config.properties`, which Maven filters at build time from the active profile's properties in `pom.xml` (default profile: `dev`).

## Setup

```bash
# 1. Start/point at a MongoDB instance, then from a mongo shell:
use wakerdb
db.createUser({ user: "wakerman", pwd: "1111", roles: [{ role: "readWrite", db: "wakerdb" }] })

# 2. Build (from waker-back/)
cd waker-back
mvn clean install
```

`mvn clean install` triggers MapStruct/Lombok annotation processing (generating `*MapperImpl` classes referenced directly by `UserApp`/`ReminderApp`), copies dependency jars to `target/dependency/` (via `maven-dependency-plugin`), and copies `src/main/webapp` into `target/classes/webapp` (via `maven-resources-plugin`) so the embedded Jetty server can locate `WEB-INF/web.xml` at runtime.

## Run Locally

There is no `mvn exec:java` binding wired up today (a commented-out `exec-maven-plugin` block in `pom.xml` shows a prior, abandoned attempt). Run the built application directly:

```bash
# from waker-back/, after `mvn clean install`
PORT=8888 mongodbHost=localhost mongodbPort=27017 \
SECONDARY_EMAIL_ADDRESS=<your-workspace-domain-email> \
java -cp target/classes:target/dependency/* com.waker.Main
```

Or run `com.waker.Main` directly from your IDE with the same environment variables set in the run configuration (this is how the original README documents it — "in IDE launch config").

On successful start you should see:

```
App launched.
Using BaseResource: ...
Server started! 🚀
```

## Email Setup (optional, only needed for registration-confirmation emails to actually deliver)

`GmailApiService` (the only email provider actually wired into `UserApp`) expects:

- A service-account private key file at classpath resource `/service-account.p12`
- The service account email hardcoded as `amin-house@appspot.gserviceaccount.com` in `GmailApiService.SERVICE_ACCOUNT_EMAIL` — **this is a personal/example account reference baked into source**, not a placeholder, and must be changed before any real deployment
- Domain-wide delegation enabled for that service account, targeting `SECONDARY_EMAIL_ADDRESS`

If these aren't configured, registration will still succeed (the mail-send failure is currently swallowed — see `api-contracts.md`), but no confirmation email will be delivered.

## Build Process

Standard Maven lifecycle. Key non-default plugin behavior to be aware of:

- **`maven-compiler-plugin`**: wires Lombok + `lombok-mapstruct-binding` + MapStruct annotation processors, in that order (order matters for Lombok/MapStruct interop).
- **`maven-dependency-plugin`** (`copy-dependencies`, bound to `package`): flattens all dependency jars into `target/dependency/` — this plus the manifest `Class-Path` entries in `maven-jar-plugin` is how the built jar resolves its classpath without a fat-jar/shade step.
- **`maven-resources-plugin`** (`copy-web-resources`, bound to `compile`): copies `src/main/webapp` → `target/classes/webapp`, which is how `Main.findWebResourceBase()` locates `WEB-INF/web.xml` via the classloader at runtime.

## Testing

**No test suite exists.** There is no `src/test/java` directory and no test dependencies (JUnit, Mockito, etc.) declared in `pom.xml`. `mvn test` will run successfully but exercise nothing. Establishing a test baseline (JUnit5 + Mockito for unit tests, Testcontainers for DAO/integration tests against a real MongoDB — or, post-migration, PostgreSQL) is called out in the roadmap as foundational work that should happen early, ideally before or alongside any structural refactor.

## Common Development Tasks

- **Add a new penalty type**: create a new `APenalty` subclass in `model/penalty/impl/`, add a corresponding constant to `Penalties`, create a matching `IPenaltyService<T>` implementation in `service/penalty/impl/`, and add a branch to `PenaltyFactory.getService(...)`. This is the one place in the codebase explicitly designed for this kind of extension.
- **Add a new endpoint**: create a new `@WebServlet`-annotated class under `web/` (or `web/auth/` if it needs a specific whitelist entry), and if it needs to bypass auth, add its exact path to `AuthFilter.AUTHORIZED_ROUTES`.
- **Inspect what the app is doing at runtime**: logging is `slf4j-simple` to console only (no file/structured logging, no log levels configured beyond defaults); several code paths also use raw `System.out.println`/`System.err.printf` (e.g. `Main`, `PenaltyApp`, all three penalty service stubs) rather than the logger.

## Known Issues Relevant to Local Development

- Hardcoded MongoDB credentials in `pom.xml` (`wakerman`/`1111`) — fine for a purely local/dogfooding setup, but must not be reused once any shared/remote environment exists.
- Hardcoded JWT secret key in `UserService` — anyone with the source can forge valid tokens; must be externalized (env var / secrets manager) before any real deployment.
- `/auth/api/test` is reachable without authentication and sends real email via the configured Gmail service account — be careful about exposing a locally-running instance to the network.
- No Docker/compose setup exists yet, so local development currently requires a manually installed JDK 17 + Maven + MongoDB. See `deployment-guide.md` for the planned containerized setup.

---

_Generated using the BMAD Method `document-project` workflow_
