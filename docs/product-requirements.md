# Product-wide requirements — Configurable Onboarding Engine

This document preserves the complete initial product specification, including scope beyond the first MVP. Feature deliverables are defined separately under `specs/`.

# Feature Specification: Configurable Onboarding Engine

**Status:** Draft / discovery. **Original feature:** product-wide. **Created:** 2026-11-15.

## Purpose

Allow an adopting organization to define onboarding journeys in a static, versioned JSON flow document, without writing new orchestration logic for each journey. A client starts a journey, receives the next customer-facing instruction or waiting state, submits answers, and the engine performs configured synchronous/asynchronous steps until the chosen completion boundary is reached.

## User stories / acceptance scenarios

### US1 — Define a journey (P1)
As an integrator, I can define and deploy a validated flow whose ordered/conditional steps mix different kinds of work.

- Given a configured flow, when the API starts the flow, then the engine loads an immutable applicable version.
- Given any interleaving of customer questions, IDV, internal checks, and provisioning, when a result arrives, then the configured next transition is evaluated centrally.
- Given invalid schema, unknown action type, or missing transition, configuration is rejected before runtime.

### US2 — Submit data and resume (P1)
As a customer, I can answer the current step and resume safely.

- The engine accepts only input for an authorized and currently allowed step; schema violations return safe errors.
- Session state survives process restart; repeated next-step requests are consistent.
- Editing a previous answer invalidates dependent data/results according to a configured, deterministic policy (reconfirmation required).

### US3 — Synchronous action (P1)
As an integrator, I configure an action that performs a REST request and consumes its typed response.

- On success, step output is stored and the engine evaluates the next transition while the caller may remain waiting for the HTTP response.
- On business rejection, the result follows a configured business path rather than being treated as an infrastructure failure.
- Network failure and timeout follow an explicitly defined technical-error policy.

### US4 — Asynchronous action (P1)
As an integrator, I configure a customer-owned REST connector that starts background processing and can be polled for completion.

- After starting the REST connector operation and receiving its polling ID, the engine records a durable `WAITING` state.
- Repeated authorized mobile `next` queries while pending return `WAIT`; the backend polls the connector independently.
- Final HTTP `200` stores mapped step output (e.g. risk score) and resumes transitions; final connector errors follow configured policies.
- Unrelated or late poll results cannot advance the wrong session. The pending-result HTTP representation (possibly `202`) remains open.

### US5 — Abuse protection (P1)
As an operator, I can protect public pre-login endpoints against automated fake sessions, OTP/SMS bombing, brute force, and provider-cost amplification.

- Before the *first* and every subsequent SMS/OTP delivery, the server enforces the configured anti-bot policy (default: require a verified CAPTCHA/bot token), independent of session or source IP novelty.
- Rate limits apply simultaneously to destination phone number, session, IP/network/device signals where available, and global/provider budgets. A fresh session or IP cannot reset destination or global limits.
- Failed verification guesses are counted separately from OTP sends, and codes are short-lived and single-use.
- Exceeded limits throttle or reject; avoid automatic long phone-number lockouts that attackers could deliberately trigger.
- The frontend is untrusted, and no costly provider call or durable workflow is initiated before mandatory admission checks pass.

### US6 — Visibility and integration (P2)
As an operator/integrator, I can observe step transitions and consume lifecycle/step events without exposing sensitive input.

## Requirements

### Confirmed from current design discussion

- **FR-001** Accept a declarative, versioned JSON flow definition identifying a journey and its steps.
- **FR-002** Each step has a defined input, output, and optional action.
- **FR-003** Steps may belong to customer input, IDV, internal/external verification, or post-decision provisioning; there is no mandatory category order.
- **FR-004** One logical flow controller manages step selection, ordering, branching, and progression.
- **FR-005** Support storage-only steps, synchronous REST actions and asynchronous actions **via customer-managed REST connectors**; execution behavior is declared per business step.
- **FR-006** A sync action can complete within an API request and allow the engine to advance.
- **FR-007** An async action starts a customer-owned REST connector job, stores its polling ID with durable `WAITING` state, exposes `WAIT` through the next-step endpoint, and resumes after backend polling sees final success/error or timeout.
- **FR-008** Persist onboarding state durably; do not require sticky application instances.
- **FR-009** Preserve selected, explicitly mapped action output for later transition logic (e.g., risk score); expose only allowlisted dynamic attributes to mobile.
- **FR-010** Distinguish business rejection from technical failure.
- **FR-011** Clients cannot choose arbitrary internal steps, skip server-validated progression, or invoke privileged adapters directly.
- **FR-020** A step is a **business-defined unit of the journey**, commonly a screen. It may include one field or many (e.g. 15 fields on one screen); fields are not automatically individual steps.
- **FR-021** A step has an input schema (possibly empty), output schema (possibly empty), and optional processing action. The same input type may be stored without further verification or checked immediately/later via synchronous or asynchronous integrations.
- **FR-022** V1 supports a **forward-only flow graph** with conditional branching. No backward edges, loops, or switching into separate subflows in v1. A runtime `WAITING` status is **not** a separate business-defined step.
- **FR-023** The engine orchestrates the **entire onboarding journey**, including eligibility decisions, customer choices, provisioning, and terminal completion/rejection; it is not limited to collecting customer data.
- **FR-012** Apply mandatory server-side anti-abuse checks before costly external operations; CAPTCHA/bot verification MUST be configurable as required for every OTP request, including the first request from a new session/IP. The initial OTP policy MUST require it. Default provider is Cloudflare Turnstile, via a replaceable provider adapter; thresholds remain configurable. See `../../docs/security-requirements.md`.

### Requirements documented in earlier architecture discussion — reconfirm scope

- **FR-013** Secure onboarding session authorization and resumption with appropriate expiry.
- **FR-014** Validate user-submitted data against the currently active step schema.
- **FR-015** Restart/resume journeys and retrieve next required customer-facing step.
- **FR-016** Encrypt temporary sensitive data, enforce retention TTL, and manage safe disposal.
- **FR-017** Invalidate dependent downstream results when earlier answers change.
- **FR-018** Coordinate async connector invocation and waiting-state persistence reliably. A transactional outbox is merely an optional implementation example, **not a required design**; delivery guarantees and implementation remain open.
- **FR-019** Support waiting on user acknowledgment when a flow requires it.

### Security requirements — OTP and resource abuse (P1)

- **SEC-001** Treat every public client and pre-auth API request as untrusted; enforce session authorization and permissible step transitions on the server.
- **SEC-002** Evaluate admission policy before creating expensive durable work or calling chargeable services (SMS, KYC, provisioning). For SMS OTP, default to server-verified CAPTCHA/bot challenge on *every send*, including the first request. Support conditional CAPTCHA policies later; never rely on IP/session reputation alone.
- **SEC-003** Use overlapping server-side throttles and quotas for destination phone (normalized), session, IP/network, device signals when available, and global/provider spend/concurrency. New session/IP MUST NOT bypass destination/global protection. Thresholds are configurable, not universal constants.
- **SEC-004** Enforce resend cooldowns and progressive throttling; do not automatically impose long phone-number lockouts that an attacker can weaponize against legitimate customers.
- **SEC-005** OTPs MUST be generated server-side, stored as non-plaintext verifier material, expire rapidly, and be single-use. Track send quotas and failed verification attempts independently.
- **SEC-006** Validate CAPTCHA tokens server-side, including expiry, one-time use, action/hostname binding when supported. Reject replay and fail closed for required challenges.
- **SEC-007** Use generic public responses where account/phone existence could be inferred; do not leak OTPs, secrets, or raw sensitive identifiers into logs.
- **SEC-008** Implement a global cost circuit breaker and provider-side spend/destination controls when supported; alert on anomalous sends and spend.
- **SEC-009** Apply strict request-size limits, endpoint authorization, abuse monitoring and relevant gateway/WAF controls. Risk scoring and mobile attestation MAY strengthen defenses but are not substitutes for baseline controls.
- **SEC-010** Verify defenses with tests for fresh-session + fresh-IP SMS flooding against a single destination, distributed floods across destinations, CAPTCHA replay, resend bursts, brute-force guesses, and provider failures.

- **SEC-011** Support configurable server-defined honeypot fields in public onboarding forms; reject or challenge unexpected nonempty values before advancing a step or triggering costly operations, with protections against autofill/accessibility false positives.
- **SEC-012** Support configurable minimum elapsed time from server-side step availability to submission; too-fast submissions MUST be blocked or challenged before costly actions. Do not trust client-reported timing.
- **SEC-013** Support different CAPTCHA experiences/providers (image selection, checkbox, passive/invisible) behind server-side validation; default initial OTP policy stays mandatory.

Security rationale, configuration knobs, and unresolved choices: `../../docs/security-requirements.md`.

## Business-defined step examples

- Personal information screen: name, address, gender, and other fields; store for subsequent steps, without requiring an immediate external verification.
- Phone or email collection screen: store submitted contact information; an optional configured completion action initiates OTP delivery after anti-abuse checks. A separate verification step presents a code field.
- Document-upload screen: accept passport, driving license, or other files; validation may execute now (sync or async) or in a later step.
- National identifier screen: BSN, SSN, etc.; verification may be sync or async based on the integration.
- System step without a customer-facing screen: issue a risk assessment or provisioning action; it may continue immediately or enter `WAITING`.

A business screen containing 15 fields is one step; a one-field BSN screen is also one step. For phone verification, the agreed model uses **two business steps**: collect phone number, then verify a user-entered SMS code. The mobile app communicates only with the onboarding API. The engine can invoke a configured REST OTP provider to initiate delivery after phone collection and validate the code when the verification form is submitted. No direct mobile-to-provider integration or client-provided verification JWT is required for this pattern.

## Example journey (illustrative, not normative)

Choose service -> phone OTP -> additional questions -> risk command -> wait for assessment -> select eligible product -> create CRM entity -> create account -> issue virtual card -> complete.

### Phone verification acceptance criteria (current design)

- Customer submits a phone number to the onboarding API. The engine stores it, advances to an **explicit CAPTCHA step** and validates that step with the configured provider. Only after the CAPTCHA step completes does it reach the OTP-send action, still subject to rate limits and SMS budgets, before the code-entry verification step.
- The verification step exposes a code input schema to the mobile client. The mobile calls only the onboarding API, which invokes the configured REST verifier with explicitly mapped data (submitted code, stored phone number, authorized context).
- On successful verification, store only allowlisted provider output, complete the step and advance according to the configured flow.
- On an invalid code, the same verification step can be returned with an error and the remaining allowed attempts. Exceeding the configured maximum can fail or terminate onboarding according to policy; avoid exploitable blanket phone-number lockouts.
- A generic HTTP 4xx is not automatically a terminal business rejection: provider-specific response mappings distinguish invalid input/attempt limits, denial and authentication/configuration faults.
- Transient HTTP 5xx / network failures use bounded, configurable retries with safe backoff; exhaustion is a technical error and must not silently be treated as business denial. Retries must account for side-effect/idempotency safety.
- Do not blindly forward request headers or arbitrary external response fields to the client. All outbound inputs, headers, credentials and exposed output attributes are allowlisted/mapped; never expose secrets.

### Agreed step action model (2026-11-15)

- A business step defines its input/output and optionally an external action; a pure storage step requires no external call.
- REST actions invoke a configured, allowlisted endpoint synchronously; async actions invoke a configured customer-owned **REST connector**, persist its polling ID, and poll that REST connector until completion while the step is in durable `WAITING` state. Broker integration is outside the engine.
- A step may initiate a configured action when its customer input is successfully submitted (e.g., send OTP after collecting phone), subject to security gates.
- Steps evaluate forward-only transitions on completion, using appropriately scoped/mapped integration outputs; no arbitrary client-supplied integration URLs or unfiltered response propagation.
- The flow must support a successful and an unsuccessful terminal outcome. On successful completion, the app receives `nextStep.type = COMPLETE_ONBOARDING`; on rejection, a separate HTTP error payload is returned with no `nextStep`. No special business `END` step is required in the public contract.

## Non-goals / not decided

- A mandatory Temporal workflow per signup is **not** a decision.
- No commitment yet to a specific language, database, or WAF. The core engine does not need to choose or implement a message broker. Cloudflare Turnstile is the initial default CAPTCHA integration, not a permanent dependency.
- No requirement that all adapters be configurable without any code.
- `COMPLETED` represents successful completion of the configured end-to-end onboarding journey, including any configured provisioning activities. The earlier data-collection-only scope is superseded by this decision.

## Cross-cutting acceptance gates

- Deterministic behavior under retries, duplicates, concurrent calls, and reordered callbacks.
- No PII in operational logs/events by default; explicit authorization and minimization required for access.
- Restart and recover an in-flight asynchronous step without double-dispatch.
- Documented threat model before exposing public endpoints.

## Open clarifications

See `../../docs/open-questions.md` for prioritized decisions; this document MUST be updated as they are resolved.

### Agreed client request/response contract (2026-11-15)

- The client interacts with **only the onboarding API**, which controls and validates step progression.
- Step submission request has `stepId`, `session`, `userInput` (a JSON object conforming to the previously returned complete step schema), and `additionalAttributes` (customer-defined dynamic integration attributes).
- Successful response contains `session` (refreshed JWT), `nextStep` (the complete JSON Schema for a form, a `WAIT` instruction, or `COMPLETE_ONBOARDING`), and `additionalAttributes`.
- No top-level `status`; JWT `exp` is not duplicated as `expiresAt`. Session renewal uses bounded sliding expiry; issuer/claims and concurrent refresh policy remain open.
- `additionalAttributes` can travel in either direction for configured integrations; only explicitly mapped, permitted outputs are exposed. Client input is never automatically trusted.
- Rejection and unrecoverable failures return a **different error payload** with HTTP error status and **no `nextStep`**; incorrect OTP can remain a recoverable validation error on the same step.
- `WAIT` is a response indicating an async operation is incomplete, with optional `retryAfterSeconds`. The app polls the next-step endpoint.
- No mandatory step execution ID / idempotency key at this stage; duplicate-submission specifics deferred.
- Integration request/response mapping belongs in internal company flow configuration, not mobile-facing step schema. See `contracts/README.md`.


### Agreed forward branching expression (2026-11-15)

- A step's `next` may be a plain target step ID for unconditional progression, or an `if` / `then` / `else` object for conditional forward progression.
- Conditions use **CEL (Common Expression Language)**, evaluated against server-controlled step results, not arbitrary executable code.
- Example: `{"next":{"if":"result.score >= 70","then":"premium-account","else":"standard-account"}}`.
- Validate and compile CEL conditions when loading a versioned flow definition, instead of reparsing them on each request.
- All referenced target steps must exist and move forward; loops/backward transitions remain out of scope for v1.
- Invalid OTP attempts, integration retries, and async timeouts are execution/error policies, not necessarily branching transitions.
- The exact CEL library is **not yet selected**.

### Async correlation and integration propagation (agreed 2026-11-15)

- **FR-024** Each onboarding session has a unique `onboardingId` claim in its trusted session JWT. The engine validates the JWT, extracts the ID, and uses it as the database session lookup/correlation key. Never trust a separately supplied ID in place of the validated claim.
- **FR-025** Propagate `X-Onboarding-ID` from that trusted value to every outbound REST call, including asynchronous connector start/poll requests.
- **FR-026** Async REST connector results must be authenticated and associated with the correct onboarding, saved polling ID and pending step before advancing. Handle stale/late results and timeout outcomes safely; exact mechanisms remain to be designed.

### Session expiration and resumption (agreed)

- Each onboarding session has a server-enforced expiration and temporary storage lifetime. Proposed default: **1 hour**, configurable per deployment/flow.
- Completing a step extends that session expiration by a configurable duration (sliding expiration). A successful API call may return a refreshed `session.jwt` as already specified; this does not by itself guarantee that persistent session data still exists.
- At session expiration, all onboarding session data, including step-attempt history and sensitive inputs, is deleted according to the deletion policy. Required external audit/retention records, if any, need a separately defined lawful retention boundary.
- Resume: verify JWT, extract trusted `onboardingId`, retrieve its session from persistent storage and verify it is unexpired; then resolve the next step. If missing/expired, reject resumption (not a new onboarding).
- Expired async callbacks must not recreate deleted sessions.

## Revisiting earlier steps (agreed conceptual behavior)

- The configured business flow is forward-only: no backward edges or loops.
- The client may offer Back navigation to an earlier completed business step when allowed. This is not a backward flow transition.
- Resubmitting an earlier step records a new attempt with integer `submission_version`, timestamp, input and outcome; the latest valid attempt is effective.
- Resubmission invalidates subsequent step executions/results, which must be recalculated or re-executed as the flow proceeds forward again. Previous attempts remain in history.
- **Point of no return (design pending):** An irreversible step (e.g., opening a bank account) must prevent navigating back beyond a defined boundary to avoid replaying irreversible actions. The schema syntax and enforcement rules are not yet decided.

### Multiple flow types (agreed)
- The engine serves organizations in any industry, not only banks. It supports multiple configured onboarding flows simultaneously (e.g., retail account creation, insurance enrollment, personal banking).
- Each flow definition declares a `type` identifying the business journey category. The type is separate from the flow definition ID and its immutable version.
- At onboarding start, the **client app explicitly supplies the flow `type`**. The engine resolves the corresponding configured flow. Server-side authorization/allowlisting and ambiguity when multiple active definitions share a type remain to be designed.

### Agreed async connector architecture (2026-11-15)

- All external engine actions use REST. For asynchronous work, a customer-owned connector bridges REST to whichever broker or background system the customer uses.
- The engine starts the operation via REST, receives a polling ID, stores it with the pending step, and polls the connector from the backend (independent of mobile next-step polling).
- A final HTTP `200` means success; explicitly mapped response fields can inform flow conditions or permitted `additionalAttributes`. Final error responses follow configured rejection vs technical-failure rules.
- **OPEN:** Should a pending poll return HTTP `202 Accepted`, another status, or a status field? Do not assume `202` is mandatory.


### Flow example decisions (2026-11-16)

- A flow JSON definition MUST declare an explicit `startStep`, naming an existing step ID. The engine MUST NOT infer the entry step from array ordering.
- Step submission history uses the field name `submission_version` (integer, incremented per submission attempt for that step within an onboarding). This is distinct from flow/schema definition versioning and database migration versions.
- CAPTCHA/bot verification MUST be enforced before initiating SMS **or email OTP/verification delivery** in the example onboarding flow, including the first attempt. The backend enforces the gate; frontend display alone is insufficient.

## Agreed flow progression and CAPTCHA step semantics (2026-11-16)

- **FR-FLOW-001:** A single resolver MUST determine what step to return from the configured flow and the persisted state/completion history; it MUST reject submissions that violate the allowed sequence.
- **FR-FLOW-002:** CAPTCHA MUST be an explicit configured step, returned through ordinary `nextStep`, validated with the configured provider server-side, then recorded as completed. No second security service dynamically selects or inserts CAPTCHA as `nextStep`.
- **FR-FLOW-003:** If CAPTCHA is required at two points, the flow definition MUST contain two corresponding step occurrences (distinct IDs). CAPTCHA tokens are verified when the relevant step is submitted; later token expiration does not revoke a verified completed step.
- **FR-FLOW-004:** Protected SMS/email integrations in the example flow MUST be reached only via the configured path containing completed CAPTCHA steps, while rate limits and spending caps remain separately enforced.
- **FR-FLOW-005:** On an incomplete async step the resolver returns `WAIT`; no forward transition is taken before successful completion.

**Illustrative sequence:** `collect-phone → captcha-before-sms → send-sms → verify-phone → collect-address`. A second send later in the journey may use `captcha-before-second-send` as another explicit step.

**Decided:** Transitions are declared separately at the flow level; the `steps` array describes step behavior.

### Optional step integration (agreed 2026-11-16)
- **FR-INT-001:** Every step defines its user-facing input using a JSON Schema where applicable. `integration` is **optional**; steps without an external call omit it entirely.
- **FR-INT-002:** When present, `integration` contains `type` (`sync-rest` or `async-rest`), `mapping`, and `connectivity` (REST URL, path, headers, payload and method as applicable).
- **FR-INT-003:** The client receives the step schema, not internal integration configuration.


## OTP initiation and confirmation — latest agreed model (2026-11-16)

- The mobile-facing step types are `USER_INPUT`, `CHALLENGE`, `WAIT`, and `COMPLETE_ONBOARDING`; internal integration configuration is never returned as a mobile-facing `INTERNAL` step.
- `initiate-otp` is a **`CHALLENGE`** step: invoking it initiates OTP delivery using its optional configured `sync-rest` integration, subject to a previously completed explicit CAPTCHA step and independent rate/cost limits. On successful initiation, the resolver returns the next configured step.
- `confirm-otp` is a **`USER_INPUT`** step: its complete JSON Schema describes the entered code; submitting it triggers its optional configured `sync-rest` verification integration. It advances only on successful verification.
- They are two separate steps, with transitions at the flow level. `integration` is optional for all steps and, when present, defines `type`, `mapping`, and `connectivity`.
- Illustrative sequence: `phone (USER_INPUT) → captcha (CHALLENGE) → initiate-otp (CHALLENGE) → confirm-otp (USER_INPUT) → address (USER_INPUT) → risk`.
- Earlier descriptions of `send-sms` as an app-facing `INTERNAL` step, and of `confirm-otp` as `CHALLENGE`, are superseded. Exact semantics for how an app initiates a challenge step without duplicating the send on retries remain a detailed-design question.

### Async risk resolution at a terminal transition (agreed)

When an async risk step is waiting, the mobile continues polling the next-step endpoint and receives `WAIT` until completion. When the result arrives, the flow resolver evaluates transitions. `COMPLETE_ONBOARDING` returns the standard HTTP 200 response with `nextStep.type` set to `COMPLETE_ONBOARDING`; `REJECT_ONBOARDING` returns the distinct error response (illustrative HTTP 403 with `ONBOARDING_REJECTED`) and no `nextStep`. Neither outcome is a business step.

## Canonical flow example (agreed conceptual baseline)

The authoritative example is `examples/flow.json` with documented assumptions in `examples/README.md`. Separate flow-level transitions, explicit CAPTCHA challenge, `initiate-otp` challenge with send-SMS REST call, `confirm-otp` user input with verify REST call, address before async risk, and terminal completion/rejection. Configured integration is invoked when its step is executed. Exact connector request/response mapping details remain implementation design, not architecture blockers.

## Future enhancement candidates (not in v1 scope)

- **Flow visualization:** Render the configured flow as a diagram showing business steps, explicit CAPTCHA/OTP challenges, integrations, and CEL conditional transitions; optionally overlay live session progress. Visualization is a potential future feature, **not a v1 requirement**.
- **Live flow editing/publishing:** Consider editing and publishing flows without redeployment in a later release. **V1 loads configured JSON flows at startup; changes require redeployment.**
