# Configurable Onboarding Engine — SDD project

Open-source, industry-agnostic onboarding engine with JSON-defined steps, separate transitions and configurable sync/async REST integrations.

## Repository layout

- `docs/product-requirements.md` — **all** agreed product requirements, beyond the first MVP.
- `docs/architecture.md` — product-wide conceptual architecture.
- `docs/security-requirements.md` — baseline security policy and threat controls.
- `docs/decisions.md` — consolidated architectural decisions / research history.
- `docs/open-questions.md` — outstanding and deferred design topics.
- `specs/001-scaffold/` — **first implementable feature**: Kotlin/Quarkus, Gradle, JDBC/PostgreSQL, Liquibase, API-first OpenAPI skeleton, tests, CI.
- `specs/002-flow-engine-mvp/` — **second feature**: generic flow engine, persisted sessions, phone collection, CAPTCHA, OTP send/confirm.
- `specs/002-flow-engine-mvp/examples/flow.json` — broader canonical reference including address and async risk, not all required in the first OTP MVP.

## First run with Claude

Extract this archive into `onboarding-bff/`, so the starting location is `onboarding-bff/onboarding-engine-sdd/`. **Launch Claude from `onboarding-bff/`** and tell it:

> Implement only T001 in `onboarding-engine-sdd/specs/001-scaffold/tasks.md` exactly, including copying the specs to the repository root. Do not start T002 or delete the original folder.

T001 produces the following repository root:

```text
onboarding-bff/
├── README.md
├── docs/
├── specs/
├── src/main/kotlin/
├── src/main/resources/
├── src/test/kotlin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── onboarding-engine-sdd/  # old copy; user deletes after verification
```

**You only delete `onboarding-engine-sdd/` after Claude confirms T001 succeeded.** Every subsequent task is read from `specs/001-scaffold/tasks.md` in the repository root.

## Working order

1. Implement `specs/001-scaffold/tasks.md` and verify its acceptance gate.
2. Review/refine `specs/002-flow-engine-mvp/tasks.md` using the working scaffold.
3. Implement the four-step MVP; tackle async risk, CEL branching and other enhancements later.

**Agreed stack:** Kotlin, Quarkus, Gradle Kotlin DSL, PostgreSQL/JDBC, Liquibase, Jackson/JSON Schema, JUnit 5, AssertJ, Testcontainers, REST Assured, SmallRye Fault Tolerance. Scheduler deferred.

**Note:** This is a lightweight SDD document layout, not a full Spec Kit CLI installation; a constitution is **not required** for this project. No live editing of flows in v1; changes require redeployment. The historical product spec and decision log are retained rather than discarded when splitting features.

## Module boundaries

Main Kotlin package: `onboarding.bff` (`src/main/kotlin/onboarding/bff/`). One deployable service, organised as internal packages. These are placeholders only; behavior arrives in feature 002.

| Package | Intended responsibility |
|---|---|
| `api` | HTTP resources (`/api/v1/onboardings`), request/response envelopes, error mapping; generic contract only |
| `flow` | Flow definition loading, session state and transition resolution |
| `steps` | Step types and runtime validation of step-specific `userInput` against JSON Schema |
| `integrations` | Outbound connectors (CAPTCHA, SMS/OTP, risk, etc.) with fault-tolerance policies |
| `persistence` | Plain JDBC repositories and Liquibase-managed schema |
| `security` | Session tokens, CAPTCHA verification hooks, input/PII protection |
| `config` | Typed configuration mappings and application wiring |

## Updating SDD documents after T001

Download the new archive as `~/Downloads/onboarding-engine-sdd.zip`, then run `./scripts/update_sdd.sh` from the repository root. The script updates only `docs/` and `specs/` and preserves application code and the root README.
