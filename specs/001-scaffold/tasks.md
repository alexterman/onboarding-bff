# Tasks — Feature 001: Scaffold

**Status:** Ready for implementation review. **Scope:** Scaffold only; do not implement onboarding flow behavior in this milestone.

**References:** `plan.md`, `spec.md`, and shared product requirements under `../../docs/`.

## Where Claude runs

Open Claude in the **parent `onboarding-bff/` directory**, not inside `onboarding-engine-sdd/`. Ask: **“Implement T001 from `onboarding-engine-sdd/specs/001-scaffold/tasks.md` exactly; do not start T002.”** T001 itself copies all specifications to the repository root and creates the application skeleton there; the user only deletes the original folder after verifying the result.

## Foundation

- [x] **T001 — Bootstrap the repository root AND initialize the application (one task, no manual moves).** The checkout starts as `onboarding-bff/onboarding-engine-sdd/` containing `docs/`, `specs/`, and `README.md`; Claude is launched from `onboarding-bff/`. First **copy** (do not symlink or delete) `onboarding-engine-sdd/docs/` → `docs/`, `onboarding-engine-sdd/specs/` → `specs/`, and `onboarding-engine-sdd/README.md` → `README.md` in the **repository root**. Preserve every document, including this task file, examples, and relative links. Also create a root-level `scripts/update_sdd.sh` that, when invoked from anywhere, reads `~/Downloads/onboarding-engine-sdd.zip`, extracts it to a temporary directory, validates that the archive contains exactly one expected `onboarding-engine-sdd/` root with `docs/` and `specs/`, and replaces only the repository-root `docs/` and `specs/` with the downloaded versions. Back up existing `docs/` and `specs/` before replacement and restore them on failure; never touch `src/`, Gradle files, CI, or the repository-root `README.md` (which may have been customized after scaffolding). Exit clearly on missing/invalid archive; support repeated runs; use paths derived from the script location, not the working directory. Include a simple dry-run or verification test that ensures code files are not modified and documents are updated. Then initialize the Kotlin + Quarkus application **in `onboarding-bff/`, not inside `onboarding-engine-sdd/`**: root-level `settings.gradle.kts`, `build.gradle.kts`, Gradle wrapper, and `src/main/kotlin`, `src/main/resources`, `src/test/kotlin`; Gradle group `io.altera`, main package `onboarding.bff`. Pin compatible dependency versions; ensure `./gradlew test` passes and the app can start with `./gradlew quarkusDev` (database setup is T004). **Do not create a second nested application or move/delete the original folder.** When done, verify root-level `docs/`, `specs/`, `README.md`, `scripts/update_sdd.sh`, `src/`, Gradle files all exist and `diff -qr onboarding-engine-sdd/docs docs` and `diff -qr onboarding-engine-sdd/specs specs` show no differences. Report that the user may now delete the original `onboarding-engine-sdd/` folder; deletion is the only manual cleanup.
- [x] **T002 — Establish module/package boundaries.** Create sub-packages of `onboarding.bff` for `api`, `flow`, `steps`, `integrations`, `persistence`, `security`, and `config`, without implementing the engine; document the intended responsibilities in the repository README.
- [x] **T003 — Configure environments safely.** Add development/test/production YAML configuration for **Quarkus runtime and PostgreSQL only** (`application.yml`, `${ENV_VAR:default}` placeholders, `%dev`/`%test`/`%prod`; no `application.properties`). Supply `.env.example` or equivalent nonsecret instructions; never commit secrets. **Do not add named OTP, CAPTCHA, risk, async-connector, or other customer integration configuration to `application.yml` or typed application config.** Each step’s optional `integration` and challenge provider/public settings are defined in its versioned flow JSON; URL/path/method/headers/payload and request/response mappings come from there. Connection secrets are resolved from protected environment variables or a secret store only when needed; flow JSON references secret names, never embeds secret values. No specific external integration is required for the scaffold.

## Database

- [x] **T004 — Configure JDBC/PostgreSQL.** Add Quarkus JDBC PostgreSQL and datasource configuration (no Hibernate/ORM); confirm the application can connect to a local PostgreSQL instance.
- [x] **T005 — Initialize Liquibase.** Configure Liquibase startup migrations and a first changelog with a minimal migration marker or scaffold-owned table; defer detailed onboarding data model to Milestone 2.
- [x] **T006 — Exercise database access.** **Decision (owner): no dedicated database test and no throwaway repository/probe class.** Following the Plumery services, there is no test that merely exercises migrations or a write/read on a placeholder table. Migrations and JDBC access are proven implicitly: every `@QuarkusTest` boots against a Testcontainers PostgreSQL (`PostgresTestResource`) with `migrate-at-start`, so the app cannot start unless Liquibase and the datasource work, and real repositories/APIs added later are tested against it. The Testcontainers resource and dependency are added in T010 when the first API tests need them. Verified manually in T004/T005: the app connects to local PostgreSQL and Liquibase creates `scaffold_marker`. No code was added for this task.

## HTTP and test foundations

- [x] **T007 — Expose health checks.** Add Quarkus health endpoints and verify the service responds when started; keep secrets/PII out of diagnostics.
- [ ] **T008 — Set up JSON contracts.** Configure Jackson and select a compatible JSON Schema validation dependency; add unit tests for JSON object serialization and validation of valid/invalid sample input (not the full flow schema).
- [ ] **T009 — Define API-first contracts and skeleton.** Write an OpenAPI contract for starting onboarding, fetching the next step, and submitting a step, with the agreed generic envelopes (`session`, `stepId`, `userInput`, `additionalAttributes` for requests; `session`, `nextStep`, `additionalAttributes` for success; separate error payloads). Model `userInput` and `nextStep.schema` as dynamic JSON objects. Add `/api/v1/onboardings` resource placeholders aligned with the contract; smoke-test with REST Assured. Do not implement flow execution yet.
- [ ] **T010 — Set up unit/integration testing.** Configure JUnit 5, REST Assured, AssertJ, Quarkus test support and Testcontainers; ensure the DB and API tests run from `./gradlew test` without external managed services.

## Delivery and review

- [ ] **T011 — Add CI.** Add a GitHub Actions pipeline that checks out the repository, sets up Java, runs Gradle build/test, and surfaces failing tests; use the Gradle wrapper and avoid storing secrets in workflow files.
- [ ] **T012 — Document local quickstart.** Add exact steps to start PostgreSQL, run migrations, start the service, and execute tests. Include a minimal local configuration template (YAML/`.env.example`) and prerequisites.
- [ ] **T013 — Scaffold acceptance gate.** From a clean checkout, verify Gradle build/start, PostgreSQL + Liquibase migration, database access via the Testcontainers-backed integration tests, health endpoint, OpenAPI contract validation, JSON validation, REST Assured smoke test, and CI pass. Record any deferred decisions without silently selecting them.

## Not part of Milestone 1

Flow loader/resolver, full JSON flow schema, session JWT, OTP/CAPTCHA integration, versioned submissions, CEL execution, async polling, expiry cleanup, retry/circuit-breaker behavior, and detailed step-specific/integration contracts are **Milestone 2 or later**. The generic public API contract belongs to Milestone 1. SmallRye Fault Tolerance is an agreed technology, but no runtime policy is needed for scaffold.

**Dependency order:** T001 → T002/T003/T004/T008 → T005 → T006; T001 → T007/T009/T010; T006/T009/T010 → T011/T012 → T013.
