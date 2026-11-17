# Onboarding BFF

A configurable, industry-agnostic onboarding engine. A client app (mobile or web) talks to one generic API; the server decides which step comes next, based on a flow defined in JSON. Steps can call external REST services (CAPTCHA, OTP, risk checks) that are configured per step in the flow, not in the application config.

**Status:** the scaffold is in place (Kotlin, Quarkus, PostgreSQL, Liquibase, OpenAPI contract, tests, CI). The flow engine itself is the next feature, so the onboarding endpoints currently answer `501 Not Implemented`.

## Endpoints

Base URL: `http://localhost:8100`

| Endpoint | Purpose | Status |
|---|---|---|
| `POST /api/v1/onboardings` | Start an onboarding for a flow `type` | Placeholder, returns `501` |
| `POST /api/v1/onboardings/next-step` | Get the current next step; also used to poll while a step is `WAIT` | Placeholder, returns `501` |
| `POST /api/v1/onboardings/steps` | Submit the user input (`userInput`) for the current step | Placeholder, returns `501` |
| `GET /swagger` | The OpenAPI contract | Working |
| `GET /q/health` (`/live`, `/ready`, `/started`) | Health checks; readiness includes the database | Working |
| `GET /q/metrics` | Prometheus metrics | Working |

Requests carry `session` (a JWT), `stepId`, `userInput` and `additionalAttributes`. Successful responses return `session`, `nextStep` and `additionalAttributes`. Failures return a separate `{ "error": { "code", "message" } }` body and never contain `nextStep`. The full contract is in [`src/main/resources/META-INF/openapi.yaml`](src/main/resources/META-INF/openapi.yaml).

## Run it locally

Prerequisites: JDK 25 and Podman with `podman-compose` (or Docker with compose).

```sh
cp .env.example .env         # local settings; git-ignored
podman machine init          # once
podman machine start         # after each reboot
podman compose up -d         # PostgreSQL on localhost:5432
./gradlew quarkusDev         # starts the service; database migrations run on startup
```

Run the tests with `./gradlew test` (they start their own PostgreSQL with Testcontainers; use `./gradlew cleanTest test` to force a re-run). `./gradlew build` also validates the OpenAPI contract. Stop the database with `podman compose down`.

If Testcontainers cannot find Docker when using Podman:

```sh
export DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')
```

## Documentation

- [`docs/`](docs/): product requirements, architecture, security requirements, decisions and open questions.
- [`specs/001-scaffold/`](specs/001-scaffold/): the scaffold feature (tasks and acceptance results).
- [`specs/002-flow-engine-mvp/`](specs/002-flow-engine-mvp/): the flow engine feature.
