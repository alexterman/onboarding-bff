# Feature 001 — Project scaffold

**Status:** Ready to implement. **Scope:** Tooling, service bootstrapping, generic API shape, tests and CI only; no business flow execution.

## Goal

Deliver a runnable single-service Kotlin/Quarkus application with PostgreSQL, migrations and an API-first contract, ready for the onboarding engine implementation.

## Bootstrap requirement

The first task must transform an initially nested SDD-document download into a single repository-root layout (`README.md`, `docs/`, `specs/`, `src/`, and Gradle files alongside each other). Claude performs every copy and source setup step from the parent repository root, preserving originals until the user removes the old folder.

## SDD update helper

T001 must install `scripts/update_sdd.sh` at the repository root. Running it should import the latest `~/Downloads/onboarding-engine-sdd.zip` into root-level `docs/` and `specs/`, with archive validation, backup/rollback on failure, and no modification to application code, build files, or the repository README.

## Acceptance criteria

1. A Gradle Kotlin DSL Quarkus application builds and runs with environment-specific settings in a YAML configuration file (`application.yml`) and no committed secrets.
3. PostgreSQL is accessible via plain JDBC; a Liquibase migration runs and a Testcontainers integration test proves a database read/write.
4. Health endpoints and placeholders for start, next-step and submit-step APIs are reachable.
5. An OpenAPI document models the generic `session`, `stepId`, `userInput`, `additionalAttributes` request; success has `session`, `nextStep`, `additionalAttributes`; failures use a separate error envelope. Dynamic input and step schema remain JSON objects.
6. Jackson/JSON Schema validation, JUnit 5, REST Assured, Testcontainers and AssertJ are configured and verified in tests.
7. CI runs build and tests from a clean checkout, and local quickstart instructions are reproducible.
8. `scripts/update_sdd.sh` safely refreshes documentation from the downloaded archive without altering application code or customized project files.

## Non-goals

No actual flow resolution, CAPTCHA, SMS, JWT lifecycle, async polling or complete persistence model; those belong to feature 002 and subsequent implementation slices.

## References

- [Product requirements](../../docs/product-requirements.md)
- [Architecture](../../docs/architecture.md)
- [MVP feature](../002-flow-engine-mvp/spec.md)
