# Open Questions — decision queue

**Policy:** Do not resolve by assumption. Work through questions one at a time with a recorded decision and rationale.

1. **RESOLVED — Scope of `COMPLETED`:** The engine owns the end-to-end journey, including configured account/card provisioning, not just data collection.
2. **Pre-auth admission:** For OTP-first or service-selection-first signup, what is the minimum state created before proving the request is legitimate? What protection can reject scripted abuse before expensive state/provider calls?
3. **Logical ownership:** Does the JSON interpreter drive *all* steps (current direction) with Temporal optional only for specialized async execution, or is a durable orchestrator part of the core?
4. **Step definitions:** RESOLVED: a step is a business-defined unit, with one or many fields, optional action, sync/async execution and forward-only branches; `WAITING` is a state, not a step kind. OPEN: full JSON grammar; conditional `next` is AGREED as plain step ID or CEL `if`/`then`/`else` targeting forward steps. DECIDED: phone entry and phone verification are two business steps; mobile submits the OTP code to onboarding API, which invokes configured REST verifier; earlier client JWT approach superseded.
5. **Flow evolution:** How are active sessions pinned or migrated when JSON definitions change?
6. **OTP integration details:** Which REST response mapping/error codes, resend/attempt-count contract, and provider authentication convention should be required? **Async lifecycle:** RESOLVED at conceptual level: start a customer-owned REST connector, store a polling ID, backend polls and completes the waiting step. OPEN: authentication, retries, timeout, pending-response contract, and handling late poll responses.
7. **Customer edits (RESOLVED):** App Back navigation may revisit completed steps even though configured flow edges are forward-only. Resubmitting creates a new attempt, invalidates all subsequent steps/results, and re-resolves forward. **OPEN:** Define a configurable point-of-no-return boundary preventing edits before irreversible side effects (e.g., account provisioning); exact representation and handling are pending.
8. **Integration scope:** All core external actions use REST, including customer-owned bridges to Kafka/other brokers. OPEN: allowlisting/SSRF controls, connector auth and deployment.
9. **Security and governance:** Session token/binding design, rate limits, consent, PII retention, audit trail, tenant isolation and regulatory requirements.
10. **Operating envelope:** Throughput, retention, multi-tenancy, deployment model, observability and API SLOs.

## How to record a decision

- Date, question, decision, alternatives, rationale, implications.
- Update `spec.md` first, then `plan.md`, contracts, tests, and tasks.

## Security policy configuration (open)

- Cloudflare Turnstile is selected as initial default; define provider-adapter contract, credentials handling, and exact policy format (default MUST protect every OTP send).
- Choose budget, destination and global rate-limit thresholds using actual traffic and provider pricing.
- Choose counter store and consistency model under distributed concurrent requests.
- Define safe customer recovery when throttled without allowing attackers to force long phone lockouts.

11. **RESOLVED — Public terminal contract:** success returns `nextStep.type = COMPLETE_ONBOARDING`; rejection returns separate HTTP error payload with no `nextStep`. Internal representation is implementation-specific.

## API contract — agreed versus unresolved (2026-11-15)

**Resolved:** `session.jwt` refreshed on successful calls; `stepId`, `userInput` object and request/response `additionalAttributes`; full step JSON Schema in `nextStep`; `WAIT` and optional polling interval; `COMPLETE_ONBOARDING`; independent error response; internal integration mappings; no duplicated expiry/status fields.

**Still open:** JWT issuance, expiration bounds, concurrent refresh behavior; concrete HTTP endpoint definitions and error codes; full async envelopes; JSON Schema dialect; duplicate submission/idempotency (explicitly deferred). See `specs/002-flow-engine-mvp/contracts/README.md`.

## Async correlation — decision update (2026-11-15)

**Resolved:** The trusted session JWT supplies `onboardingId`; the engine uses it as DB lookup/correlation key and propagates `X-Onboarding-ID` as a header on all outbound REST calls (including async connectors).

**Revised:** Async uses a customer-owned REST connector and a saved polling ID. OPEN: pending-poll HTTP contract (is `202` necessary?), connector authentication, backend poll schedule, timeout, retries, stale results and safe restart. Outbox merely an optional example; Kafka envelope is outside the engine.

## Session lifetime (decided / open)

**Decided:** Configurable onboarding expiration (suggested initial default 1 hour); completed steps extend expiry by a configurable duration; expired session-scoped data is deleted. Resume requires validated JWT `onboardingId` plus existing unexpired DB state.

**Open implementation details:** Cleanup scheduling, JWT/session expiry synchronization, whether polling during WAIT extends the session, and separately mandated legal/audit retention.

## Multiple flow types
**Agreed:** Concurrent flow definitions, each with a business `type`; the client app supplies `type` in the start request. The engine is industry-agnostic, not bank-specific. **Open:** Server-side validation/authorization of requested type and handling multiple active definitions of the same type.

## Async REST connector — pending response (OPEN, 2026-11-15)

The start endpoint returns a polling ID. Subsequent polls must distinguish pending from completed success (`200`) and final failures. **Whether HTTP `202 Accepted` represents pending is explicitly undecided.** Do not codify it as mandatory until we agree.


## Flow JSON baseline — decisions (2026-11-16)

**Resolved:** Explicit `startStep` references a defined step. Step history uses integer `submission_version`. Require CAPTCHA before sending SMS or email verification codes/links; further bot policy optimization is later.

## Resolved — CAPTCHA and flow progression (2026-11-16)

- One flow resolver is responsible for the next step, from JSON configuration and persisted step history.
- CAPTCHA is a normal explicit step, possibly repeated at distinct points with distinct IDs; completion is recorded after server-side provider verification.
- No hidden injection of CAPTCHA steps by another progression controller. Independent rate/spend controls are retained.

**RESOLVED:** Transitions are a separate flow-level `transitions` collection.


### Latest OTP decision and remaining detail (2026-11-16)

**Decided:** `initiate-otp` = `CHALLENGE`; `confirm-otp` = `USER_INPUT`, both with optional REST integrations; no `INTERNAL` type exposed to the mobile.
**Detailed design still open:** precise invocation/acknowledgment contract for OTP initiation and protection against repeat sends during retry/polling.

## Architecture review closure — 2026-11-16

**Resolved:** Separate flow-level transitions, invocation of optional integrations upon step execution, and a canonical illustrative `examples/flow.json`. **Deferred to implementation:** exact REST response mappings, pending async poll HTTP signal (including 202), connector error wire format and transport-level reliability/duplicate handling. These do not block technical planning.
