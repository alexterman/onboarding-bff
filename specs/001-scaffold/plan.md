# Implementation plan — 001 Scaffold

**Status:** Ready for implementation.

## Initial workspace layout and bootstrap

Initially, the downloaded archive is extracted as `onboarding-bff/onboarding-engine-sdd/{README.md,docs/,specs/}`. **T001 is responsible for restructuring the repository itself**, without manual file copying: copy those documents to `onboarding-bff/{README.md,docs/,specs/}` and create Gradle and `src/` alongside them in `onboarding-bff/`. Keep the original folder untouched; the user deletes it only after T001 confirms the copied files and runnable scaffold. T001 also creates `scripts/update_sdd.sh` to refresh root-level `docs/` and `specs/` from `~/Downloads/onboarding-engine-sdd.zip` on subsequent releases, with backup/rollback and strict protection of code/build files. All later tasks operate from `onboarding-bff/` root.

## Delivery

Use one deployable Kotlin + Quarkus application and Gradle Kotlin DSL. Set up API-first HTTP skeleton, persistence connectivity, migrations, tests and CI. No working onboarding flow in this feature.

## Agreed stack

| Concern | Decision |
|---|---|
| Language | Kotlin |
| Framework | Quarkus |
| Build | Gradle with Kotlin DSL |
| Application configuration | YAML (`src/main/resources/application.yml`) via `quarkus-config-yaml`; no `application.properties`; follows the conventions of the sibling Plumery services (`~/dev/plumery`): `application.yml`, every environment-specific value as `${ENV_VAR:default}`, `devservices` disabled, graceful shutdown. HTTP port is `8100` (`HTTP_PORT`), not the Quarkus default 8080 (avoids clashes with other Plumery services); test port is `18100`; no management port for now; environment-specific values via profiles (`%dev`, `%test`, `%prod`) and `${ENV_VAR}` placeholders |
| Primary storage | PostgreSQL; JSONB where useful |
| Database access | Plain JDBC; no ORM |
| Schema migrations | Liquibase |
| JSON | Jackson; compatible JSON Schema validator |
| Tests | JUnit 5, Testcontainers, REST Assured |
| Integration resilience | SmallRye Fault Tolerance (bounded retries/circuit breakers) |
| CAPTCHA default | Cloudflare Turnstile, replaceable provider |
| Conditional branching | CEL expressions; concrete Java implementation to choose |

**Customer integrations (critical boundary):** `application.yml` is for service infrastructure (Quarkus HTTP/runtime, PostgreSQL and other operational settings), **not per-customer or per-provider connectors**. Do **not** configure `onboarding.integrations.captcha`, `otp-delivery`, `async-connector`, or equivalent named connector sections in application YAML or application configuration classes. The versioned flow JSON is authoritative for each step’s optional `integration: { type, mapping, connectivity }`, including REST endpoint, HTTP method, path, headers and payload. CAPTCHA provider/public challenge configuration belongs in its flow step. Runtime secrets can be looked up securely by symbolic reference using environment variables or a secret store, not hardcoded in flow files or exposed to clients. Scaffold does not implement external connectors.

**Deployment:** One deployable service is the v1 planning assumption (not a separately ratified topology). Organize responsibilities as internal packages/modules. **Deferred:** Scheduler/worker technology for async REST polling and expiry cleanup; deployment environment; exact connector wire contracts. No broker dependency is required inside the engine.

## Architecture boundaries

Gradle group ID is `io.altera`; the main Kotlin package is `onboarding.bff` (`src/main/kotlin/onboarding/bff/`). Create sub-packages under it for `api`, `flow`, `steps`, `integrations`, `persistence`, `security`, and `config`. Avoid implementing their behavior until feature 002. Keep the public API contract generic; step-specific `userInput` is dynamically validated at runtime.

## Acceptance gate

Working Gradle build, PostgreSQL/JDBC with Liquibase, Testcontainers database round-trip, OpenAPI-validated API placeholders, health endpoint, tests and CI from clean checkout.

## Deferred

Scheduler for polling/cleanup, CEL library implementation, live flow updates and visualization. See [open questions](../../docs/open-questions.md).
