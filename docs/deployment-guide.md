# Waker (waker-back) - Deployment Guide

**Date:** 2026-07-27

## Current State: No Deployment Configuration Exists

A full repository scan found:

- No `Dockerfile` or `docker-compose.yml`
- No CI/CD pipeline configuration (no `.github/workflows/`, no `.gitlab-ci.yml`, no `Jenkinsfile`)
- No infrastructure-as-code (no Terraform/Pulumi/CloudFormation)
- No deployment scripts of any kind

The only "deployment" artifact the build produces is a runnable jar plus a sibling `target/dependency/` folder of jars (via `maven-jar-plugin` + `maven-dependency-plugin`'s `copy-dependencies` goal, referenced from the jar's manifest `Class-Path`). Running it requires a JDK 17 runtime, network access to a MongoDB instance, and the environment variables listed in `development-guide.md` to be set in whatever process manager/host launches it. This is effectively a "run it on a VM by hand" deployment model — there is no evidence in the repository of this project ever having been deployed anywhere beyond a developer machine.

## Planned Deployment Approach (from ASSESSMENT_AND_ROADMAP.md)

The agreed direction (see the roadmap document for full detail and current phase status) is:

1. **Containerize the backend** with a multi-stage `Dockerfile`:
   - Build stage: Maven + JDK image, runs `mvn clean package`
   - Runtime stage: slim JRE image, copies the built jar + `target/dependency/`
2. **Add a `docker-compose.yml`** for local development that runs:
   - The backend container
   - A PostgreSQL container (replacing the current requirement for a manually-provisioned MongoDB instance)
   - Environment variables sourced from a `.env` file (gitignored), replacing today's hardcoded `pom.xml` credentials and hardcoded JWT secret
3. **Migrate persistence from MongoDB to PostgreSQL**, with Flyway or Liquibase managing schema migrations from the start (there is no equivalent versioning today).
4. Longer-term: a real deployment target (managed Postgres + a container host) once the app has actual users, at which point CI/CD (build + test + image publish) should be introduced.

Until this work lands, **there is no "how to deploy this to production" answer** — this guide will be updated once the Docker/Postgres work (Phase 1 of the roadmap) is complete.

## Configuration That Will Need Externalizing Before Any Real Deployment

Carried over from `architecture.md`'s "Known Architectural Issues," repeated here because they are deployment blockers, not just code-quality concerns:

- JWT signing secret (currently a hardcoded byte array in `UserService`)
- MongoDB (or future PostgreSQL) credentials (currently hardcoded in `pom.xml` profiles)
- Google service-account credentials for email sending (currently a bundled `.p12` file reference + hardcoded service-account email string)
- CORS policy (currently wide open, `Access-Control-Allow-Origin: *`, unconditionally)
- The `/auth/api/test` unauthenticated email-send endpoint must be removed or restricted before any environment is network-reachable by anyone other than the developer

---

_Generated using the BMAD Method `document-project` workflow_
