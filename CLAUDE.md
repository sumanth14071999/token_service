# CLAUDE.md

Context for AI coding assistants working in this repo.

## Project
Spring Boot 3.3 / Java 21 service that answers real-time RFID token authorization requests from CPOs,
and consumes token lifecycle events from Kafka. `README.md` is the spec (authorization rules, expiry,
security requirement, production setup). Read it before changing behavior.

## Commands
- Build and unit tests: `mvn test`
- One test class: `mvn test -Dtest=TokenCacheTest`
- Local infra: `docker compose up -d`, then `mvn spring-boot:run`

## Database migrations (Liquibase)
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`.
- One **formatted SQL** file per change in `db/changelog/changes/`, named `NNN-description.sql`, and
  included from the master changelog in order.
- Every changeset has an `author:id` and a `--rollback`.
- **Never edit a changeset that has been applied.** Add a new one.

## Conventions
- Time: `Instant` (UTC) from the injected `Clock`. Business dates (like card expiry) have a time zone:
  see README.
- The authorization endpoint is latency-critical (README "Production"). Think before adding work to it.
- Every behavior change comes with a test. Prefer plain unit tests; use Testcontainers for DB/Kafka
  behavior.
- Constructor injection only.
