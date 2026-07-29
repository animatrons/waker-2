# Waker Backend (greenfield rewrite)

Spring Boot **4.1.x** on **Java 25**, PostgreSQL 17, Flyway, Spotless.

## Local stack (preferred)

From the **repository root** (not this directory):

```bash
cp .env.example .env   # edit passwords if desired
docker compose up --build
```

Services:

| Service | Role | Ports (defaults) |
| --- | --- | --- |
| `waker-backend` | Spring Boot app | `8080` |
| `postgres` | PostgreSQL 17 | host `5433` → container `5432` (override via `POSTGRES_PORT`) |
| `mailpit` | SMTP catcher (UI) | `1025` / `8025` |

The backend waits for Postgres to become healthy before starting. JDBC uses the Compose service hostname `postgres:5432` (internal network), not the host-mapped port.

## Formatting (Spotless)

Spotless (`google-java-format`) runs on `mvn verify`. To fix locally:

```bash
mvn -f waker-backend spotless:apply
```

From this directory:

```bash
mvn spotless:apply
```

Or via Docker if you don't have JDK 25 / Maven installed:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.11-eclipse-temurin-25 \
  mvn -q spotless:apply
```

## Tests

Integration tests use a **shared singleton** PostgreSQL 17 Testcontainers instance (see
`AbstractIntegrationTest`). ArchUnit AD-1 module-boundary rules run in the same suite.

```bash
# from repo root, with Docker available for Testcontainers:
docker run --rm \
  -v "$PWD":/workspace -w /workspace/waker-backend \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --network host \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  maven:3.9.11-eclipse-temurin-25 \
  mvn -B test
```

Or with a local JDK 25 + Maven: `mvn -f waker-backend verify` (Spotless + tests + ArchUnit).

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs on PRs to `main`: build/Spotless/tests and a
gitleaks secret scan scoped to the rewritten codebase (legacy `waker-back/` / `waker-front/`
excluded via `.gitleaks.toml`). Enable branch protection on `main` with required checks
`build-test` and `secret-scan` to block merges on failure.

## Legacy note

`../waker-back` is the old Jetty/Mongo codebase and is **not** used by this module.
