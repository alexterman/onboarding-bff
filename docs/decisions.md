# Research / Architecture Decision Log

**Status:** Work in progress. Entries reflect design discussion, not externally validated benchmarks.

| ID | Topic | Current position | Status |
|---|---|---|---|
| ADR-001 | Flow definition | JSON, versioned, company-configurable | Agreed direction |
| ADR-002 | Flow control | One logical owner of step graph and next-step progression | Agreed direction |
| ADR-003 | Category ordering | IDV, customer data, checks and provisioning may interleave | Agreed |
| ADR-004 | Step contract | Each step has input, output and optional action | Agreed direction |
| ADR-005 | Sync execution | REST invocation, process response and advance | Agreed direction |
| ADR-006 | Async execution | REST connector returns polling ID; engine persists WAITING and polls connector | Revised/agreed 2026-11-15 |
| ADR-007 | Persistence | Session state durable; app nodes can be stateless | Agreed direction |
| ADR-008 | Temporal | Not inherently required; premature idle workflow creation is a concern under attack | Open technology choice |
| ADR-009 | Reliability | Reliable REST connector start/poll, durable state and error handling needed; outbox remains merely an example | Design pending |
| ADR-010 | Security | Gateway, admission, rate limits, bot measures, OTP quotas | Proposed layered design |
| ADR-012 | CAPTCHA provider | Cloudflare Turnstile as initial default, behind pluggable provider interface; server validation before each OTP send | Agreed 2026-11-15 |
| ADR-011 | Lifecycle completion | Engine orchestrates end-to-end onboarding, including configured provisioning | Agreed 2026-11-15 |

| ADR-013 | Business step granularity | Step = business-designed screen/activity; one or many fields | Agreed 2026-11-15 |
| ADR-014 | Flow graph v1 | Forward-only with conditional branching; no loops or subflows | Agreed 2026-11-15 |
| ADR-015 | Execution status | WAITING is a runtime state, not a separate step type | Agreed 2026-11-15 |

| ADR-016 | Phone proof step | Separate collect-phone then verify-phone business steps; OTP send/validate through engine-configured integrations; mobile communicates only with onboarding API; earlier JWT-only design superseded | Revised 2026-11-15 |

## Risks to investigate

- Adversaries creating huge numbers of anonymous sessions and idle workflows.
- Fake OTP starts and verification brute force causing direct costs.
- Connector response forgery, stale polling results, and cross-session correlation leaks.
- JSON configuration accidentally allowing SSRF or arbitrary internal network calls.
- Flow version changes while sessions are in flight.
- Complex conditional loops or invalidation cycles.
- Deleting expired sessions while provisioning side effects remain outstanding.

## External methodological reference

- GitHub Spec Kit: https://github.com/github/spec-kit ; its project-wide constitution and per-feature spec/plan/tasks conventions inform document placement only. This is not a vendor design endorsement.

| ADR-017 | Storage/REST/async steps | Storage-only and integration-backed business steps; action configuration specifies transport, mapped inputs, headers and outputs | Agreed 2026-11-15 |
| ADR-018 | REST response policies | Explicit provider business error mapping; invalid code may retry same step; bounded technical retries; no blind header/payload forwarding | Agreed 2026-11-15 |
| ADR-019 | Completion representation | Terminal successful/unsuccessful outcomes required; special END step versus terminal edge unresolved | Open 2026-11-15 |
| ADR-020 | On-submit action | Configured side effect can run after form submit, e.g., OTP send after phone collection; enforce admission controls | Agreed direction 2026-11-15 |

## Agreed API contract decisions (2026-11-15)

| ID | Decision | Status |
|---|---|---|
| ADR-021 | Step request includes `stepId`, `session`, `userInput` object and `additionalAttributes` | Agreed |
| ADR-022 | Step response includes refreshed `session.jwt`, `nextStep`, `additionalAttributes`; no separate `status` or duplicated JWT `expiresAt` | Agreed |
| ADR-023 | Form `nextStep` returns full JSON Schema, not a list of fields; `userInput` validated server-side | Agreed |
| ADR-024 | `WAIT` is a `nextStep` instruction with optional poll interval | Agreed |
| ADR-025 | `COMPLETE_ONBOARDING` is a success instruction; terminal rejection is a separate HTTP error payload with no `nextStep` | Agreed |
| ADR-026 | Client and server may exchange dynamic `additionalAttributes`; integrations must use explicit mappings and trust boundaries | Agreed |
| ADR-027 | `session.jwt` refreshed on each successful request, subject to absolute expiry / concurrent refresh safeguards | Agreed direction |
| ADR-028 | No additional public step execution ID or idempotency-key requirement for now | Deferred deliberately |

**Supersedes:** ADR-019's unresolved public terminal representation: client uses `COMPLETE_ONBOARDING` on success and a separate error payload on rejection. Whether internal terminal nodes are used is an implementation detail. Previous contract placeholder responses with top-level `state` / `sessionId` are superseded.

## Decision — CEL forward branching (2026-11-15)

**Agreed:** Keep `next` as either a literal step ID or a simple `if`/`then`/`else` expression evaluated with CEL against current step results. For example, `result.score >= 70` selects `premium-account`, otherwise `standard-account`. Flow definitions are validated and CEL expressions compiled on loading. v1 remains forward-only (no loops/backward edges). Execution failures and retry policies are handled separately from business branching. A specific CEL library/engine language is not selected yet.

## Decision — Async correlation and dispatch (2026-11-15)

| ID | Decision | Status |
|---|---|---|
| ADR-030 | Use the `onboardingId` from the validated session JWT as the onboarding database lookup key and common integration correlation ID | Agreed |
| ADR-031 | Propagate `X-Onboarding-ID` on outbound REST requests and equivalent header/metadata on async messages | Agreed |
| ADR-032 | Async results update the waiting step; subsequent app polling resolves the next step via the flow | Agreed |
| ADR-033 | Transactional outbox is **an example option**, not a committed implementation; reliable dispatch strategy TBD | Open |

Security caveat: correlation is not authorization. Authenticate incoming provider results and distinguish pending steps/actions when needed.

## Decision — Session lifetime and recovery (2026-11-15)

| ID | Decision | Status |
|---|---|---|
| ADR-034 | Session expiration is configurable; **1 hour** is the proposed default | Agreed (default provisional) |
| ADR-035 | Each completed step extends session expiration by a configurable duration | Agreed |
| ADR-036 | Expiration deletes all session-scoped onboarding data, including attempts; any legally required audit retention is a separate policy | Agreed principle |
| ADR-037 | Resume validates session JWT, extracts `onboardingId`, checks an existing unexpired DB session and resolves the next step; missing/expired sessions cannot resume | Agreed |

Open: cleanup mechanism, coordination of token renewal with database expiry, and behavior on idle polling / WAIT without a completed step.

## Decision — Back navigation, downstream invalidation, irreversible boundaries (2026-11-15)

**Agreed:** Flow edges remain forward-only. The app may navigate back to edit an earlier completed step; resubmission becomes a new numbered attempt, and all subsequent steps/results are invalidated. The latest valid attempt is authoritative. **Design later:** A configurable point of no return must prohibit navigating back past irreversible operations such as opening a bank account. Exact shape/behavior is unresolved.

## Decision: multiple concurrent flow types
Agreed: Support multiple onboarding flows concurrently across industries. Each flow definition has a business `type` (e.g., RETAIL_SIGNUP, PERSONAL_BANKING), separate from its ID and version. The client app sends the requested `type` when starting onboarding. Validating/authorizing that type and resolving multiple versions remain open for detailed design.

## Decision — Async through REST connectors (2026-11-15)

- **Agreed:** The engine does not implement Kafka/RabbitMQ clients. A customer-provided REST connector integrates with its own asynchronous infrastructure.
- Start the job with REST; store the returned polling ID and mark the step waiting. Backend polls the connector; mobile polling remains a separate mechanism. Final HTTP 200 is success; configured errors are failures (technical retries remain policy-driven).
- **Open question:** Is HTTP `202 Accepted` required for still-processing, or can another explicitly configured response represent pending?
- Transactional outbox is not an adopted design decision.


### 2026-11-16 — Flow entry, attempt naming, OTP gates

**Agreed:** Explicit `startStep` in each flow definition. The integer per-step attempt counter is named `submission_version` (not a schema version). CAPTCHA verified server-side before SMS **and email verification/OTP** dispatch; this includes the first send.

### ADR-045 — Single resolver and explicit CAPTCHA steps (agreed 2026-11-16)

The JSON flow and the database's step-completion history are the source of truth for all step progression. The resolver validates the path through the last completed step and determines the next configured step (or `WAIT`). CAPTCHA is a regular, explicitly declared step: successful provider verification completes that step. Two required CAPTCHA challenges mean two distinct steps. No secondary policy engine inserts or determines `nextStep`. Independent rate limits, quotas and spending controls remain necessary.

**Not yet decided:** Flow-level `transitions` vs step-level `next` representation.

### Decision (2026-11-16): optional step integrations
Steps have an explicit input schema, while the `integration` object is optional and omitted for steps requiring no external call. If configured it uses `type` (`sync-rest`/`async-rest`), `mapping`, and `connectivity` (URL, path, method, headers, payload).


### 2026-11-16 — OTP initiation and confirmation classification (agreed)

`initiate-otp` is a mobile-facing `CHALLENGE` step with an OTP-delivery REST integration; `confirm-otp` is a `USER_INPUT` step with a code schema and an OTP-verification REST integration. The next business step after successful phone confirmation is address collection; risk assessment follows address. They are separate steps with transitions at flow level. The previous example using an app-facing `INTERNAL` send-SMS step and `CHALLENGE` confirmation is superseded. Integration remains optional for any step.

### 2026-11-16 — Resolving a WAIT into terminal outcomes (agreed)

`COMPLETE_ONBOARDING` and `REJECT_ONBOARDING` are terminal **flow-transition outcomes**, not step definitions. After a risk step waits asynchronously, mobile polling uses the ordinary next-step endpoint: on approval the engine returns HTTP 200 with `nextStep.type = COMPLETE_ONBOARDING`, refreshed `session`, and permitted `additionalAttributes`; on rejection it returns a separate error response (illustrative HTTP 403, `ONBOARDING_REJECTED`) with no `nextStep`.

### Decision — canonical sequential example and readiness (2026-11-16)

- Maintain `examples/flow.json` as the baseline example: phone → CAPTCHA → initiate OTP → confirm OTP → address → async REST risk → complete/reject.
- `steps` describe execution; separate `transitions` define the next step and CEL branching.
- Invoking a step invokes its optional configured integration; no `onEnter`/`onSubmit` configuration. `INTERNAL` steps are executed server-side and not returned as a mobile `nextStep`.
- OTP initiation is a client-facing `CHALLENGE` triggering its REST send action; confirmation is `USER_INPUT` triggering its REST verify action.
- Remaining specifics (connector response-mapping DSL, async pending HTTP status, exact error contracts) are explicitly deferred to detailed design/implementation. Conceptual architecture considered complete.

### Decision — Kotlin + Quarkus (2026-11-16)

Use Kotlin as the implementation language and Quarkus as the application framework. Other infrastructure choices remain open.

### Decision — YAML application configuration (2026-11-17)

Application configuration uses YAML (`src/main/resources/application.yml`, via `quarkus-config-yaml`) instead of `application.properties`. Environment differences use Quarkus profile sections and environment-variable placeholders; secrets are never committed. Java 25 (LTS) is the build/runtime target (verified with Quarkus 3.40.1, Kotlin 2.4.21).

### Decision — Group ID and main package (2026-11-17)

Gradle group ID is `io.altera`; the main Kotlin package is `onboarding.bff`, with internal packages (`api`, `flow`, `steps`, `integrations`, `persistence`, `security`, `config`) beneath it.

### Decision — Port and configuration style (2026-11-17)

The service does not use Quarkus default port 8080: HTTP `8100`, overridable via `HTTP_PORT`. Test port is `18100`. A management port is intentionally not configured for now. Configuration follows the Plumery services' conventions (`application.yml`, `${ENV_VAR:default}` for environment-specific values, devservices disabled, graceful shutdown).
