# Scaffold acceptance gate — Feature 001 (T013)

**Run:** 2026-11-17, from a fresh `git clone` of commit `c1be1b0` ("task 012"), JDK 25, Podman.

| Gate item | Result | Evidence |
|---|---|---|
| Gradle build from clean checkout | PASS | `./gradlew build --no-daemon`: 15 tasks executed, BUILD SUCCESSFUL |
| Application starts | PASS | `java -jar build/quarkus-app/quarkus-run.jar` with `DB_PASSWORD` set; listens on 8100 |
| PostgreSQL + Liquibase migration | PASS | `001_create_scaffold_marker_table` recorded in `databasechangelog` after startup |
| Database access via Testcontainers-backed integration tests | PASS | `OnboardingsResourceShould` and `OpenApiContractShould` boot the app against Testcontainers PostgreSQL (no dedicated DB test, by decision, see T006) |
| Health endpoint | PASS | `/q/health/ready` returns 200 (includes database check) |
| OpenAPI contract validation | PASS | `./gradlew openApiValidate` ("Spec is valid"), wired into `check`; a deliberately broken file fails the task |
| OpenAPI served and used | PASS | `/swagger` returns 200; resource implements the interface generated from `openapi.yaml` |
| REST Assured smoke test | PASS | 6 tests, 0 failures (placeholders answer 501 with the error envelope; bad requests 400) |
| JSON validation | PASS (manual) | JSON Schema validator `networknt:2.0.8` verified with a throwaway test; no committed test (see T008) |
| CI | **NOT VERIFIED** | `.github/workflows/ci.yaml` exists and parses, but the repository has no GitHub remote, so it has never run |

## Deferred decisions (recorded, not selected)

- JSON Schema dialect (validator supports several; see `docs/open-questions.md`).
- Final endpoint paths and methods; `POST /api/v1/onboardings/next-step` is a candidate, and `next-step` may become a `GET`.
- Error code and status taxonomy; only `NOT_IMPLEMENTED` and `INVALID_REQUEST` exist.
- Typed `nextStep` model: `oneOf` currently generates `typealias NextStep = Any`; needs a discriminator.
- Exception-handling design for the domain (own approach, not Plumery's `ApiException`).
- Whether `/q/metrics` should be restricted or moved to a management port (currently on the main port).
- `@Timed` on `suspend` resource methods may not measure full execution.
- WireMock and per-step integration test resources arrive with feature 002 connectors.
- JWT/session handling, CAPTCHA, OTP, async polling and persistence model: feature 002.

## Spec drift to resolve

- `spec.md`/`plan.md`/`tasks.md` still mention `%dev`/`%test`/`%prod` profiles; the project uses none (test overrides live in `src/test/resources/application.yml`).
- `docker-compose.yml` is the local PostgreSQL entry point and is documented in the README; it is not exercised by CI (tests use Testcontainers).
