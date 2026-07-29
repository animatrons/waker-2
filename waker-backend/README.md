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
./mvnw spotless:apply
```

Or via Docker if you don't have JDK 25 / Maven installed:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.11-eclipse-temurin-25 \
  mvn -q spotless:apply
```

## Legacy note

`../waker-back` is the old Jetty/Mongo codebase and is **not** used by this module.
