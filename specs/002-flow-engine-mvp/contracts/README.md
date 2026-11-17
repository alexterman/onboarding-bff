# Onboarding API — agreed conceptual contract

**Status:** Conceptually agreed on 2026-11-15. Examples are illustrative; exact OpenAPI, error codes and field formats remain to be specified.

## Request: submit current step

The client sends the step identifier, the user's submitted object, optional customer-specific integration attributes, and session information. `userInput` must validate against the **complete JSON Schema** returned for that step. The server determines which step is actually allowed.

```json
{
  "stepId": "verify-phone",
  "session": { "jwt": "eyJ..." },
  "userInput": { "verification": { "code": "1234" } },
  "additionalAttributes": { "verificationId": "abc123" }
}
```

- `session` holds session credentials/metadata. The signed JWT is authoritative; never trust arbitrary client-provided session identifiers. The engine returns a refreshed JWT with every successful call, with bounded sliding expiry; `exp` is already in the JWT, so no duplicated `expiresAt` field.
- `userInput` is a JSON **object**, not a list of fields or a client-supplied schema.
- `additionalAttributes` is a customer-defined bag of dynamic data for configured integrations, available in **both** request and response. Client-supplied values are untrusted and must not replace server-trusted values.

## Response: next user action

```json
{
  "session": { "jwt": "eyJ...refreshed" },
  "nextStep": {
    "id": "address",
    "type": "FORM",
    "schema": {
      "type": "object",
      "properties": {
        "address": {
          "type": "object",
          "properties": {
            "street": { "type": "string" },
            "city": { "type": "string" }
          },
          "required": ["street", "city"]
        }
      },
      "required": ["address"]
    }
  },
  "additionalAttributes": { "customerId": "12345" }
}
```

`nextStep.schema` contains the **complete schema** needed by the frontend, not a field list or schema reference. The engine owns and validates against the corresponding authoritative version.

## Waiting

```json
{
  "session": { "jwt": "eyJ...refreshed" },
  "nextStep": { "type": "WAIT", "retryAfterSeconds": 3 },
  "additionalAttributes": {}
}
```

The app polls the next-step endpoint; `retryAfterSeconds` is optional advice, not a guarantee. `WAIT` is a response instruction / execution state, **not** a business step type. There is no redundant top-level `status`.

## Successful end of journey

```json
{
  "session": { "jwt": "eyJ...refreshed" },
  "nextStep": { "type": "COMPLETE_ONBOARDING" },
  "additionalAttributes": { "customerId": "12345", "accessToken": "eyJ..." }
}
```

The final app token is an *example*, not necessarily issued by the engine: trusted integrations can make explicitly mapped outputs available to the app. `COMPLETE_ONBOARDING` is an app-facing instruction; the engine records a terminal outcome internally.

After an asynchronous risk step returns `WAIT`, the mobile polls the existing next-step API. Once the backend finishes and the flow resolver selects `COMPLETE_ONBOARDING`, the response is HTTP 200 with the normal `session`, `nextStep: { "type": "COMPLETE_ONBOARDING" }`, and `additionalAttributes`. If it selects `REJECT_ONBOARDING`, respond with a separate HTTP error payload (illustrative HTTP 403 and `ONBOARDING_REJECTED`); never return a `nextStep` on rejection. `COMPLETE_ONBOARDING` and `REJECT_ONBOARDING` are **terminal transition outcomes, not step definitions**.

## Errors and business rejection

No `nextStep` in error responses. Onboarding rejection returns an appropriate HTTP error and a **separate** error payload. HTTP 403 with `ONBOARDING_REJECTED` is the agreed illustrative risk-rejection response; the exact general error-code/status mapping remains open:

```json
{
  "error": {
    "code": "ONBOARDING_REJECTED",
    "message": "We cannot complete your onboarding."
  }
}
```

A wrong OTP is a recoverable validation failure: it may return the same phone-verification form with a safe validation error and remaining attempts, instead of terminating the journey. Provider HTTP 4xx alone does not imply business rejection. Technical 5xx/network failures use bounded, configured retry policy, then an appropriate technical error. Error details must not expose sensitive internal decisions.

## Internal integration mappings (not sent to the mobile app)

Company-supplied **flow JSON (not application.yml)** declares which REST endpoint or async REST connector is invoked, which **explicitly allowlisted/mapped** fields from current input, trusted stored data, session context and permitted additional attributes are forwarded, and how outputs are used. Internal results can drive transitions without being exposed. Only selected integration outputs map to response `additionalAttributes` when the mobile app needs them. Never blindly forward entire request headers, secrets, or provider payloads.

Example pattern: phone collection stores the number and triggers a protected OTP-send action; phone verification presents a code schema and invokes a configured synchronous REST validator; on valid response advance to address collection. The mobile communicates **only** with the onboarding API.

## Deliberately deferred / still open

- Separate public `stepExecutionId` or idempotency-key requirements and duplicate-submission semantics (intentionally deferred, not implicitly solved).
- Precise endpoint paths/methods, exact JSON Schema dialect, versioning and error taxonomy.
- JWT issuer, claims, absolute expiry, renewal/rotation concurrency policy and credential storage.
- Async REST connector start/poll contract, saved polling ID, authentication, timeout semantics, safe retries and exact pending response (HTTP `202` is a candidate, not agreed).
- Field names in examples are candidates pending formal OpenAPI validation.

## Agreed correlation contract (2026-11-15)

- Extract `onboardingId` from validated `session.jwt` for DB lookup; don't accept an unverified override.
- Outbound REST: set `X-Onboarding-ID` request header from the trusted ID.
- Outbound sync and async connector REST requests include `X-Onboarding-ID`. The engine stores the connector-provided polling ID with the waiting step. No direct Kafka protocol contract is required in the core engine.
- Transactional outbox remains a possible implementation, not a decision.

## Session resume and lifetime

- The engine validates the JWT from `session`, reads its `onboardingId`, and only resumes if the DB session exists and has not expired. Missing/expired means an error response, never a silently recreated session.
- The engine refreshes the JWT in successful responses as previously agreed. Completing a step extends the DB session expiry by a configurable duration; default session lifetime is proposed at one hour. JWT expiry is not duplicated as a separate `expiresAt` response field.

## Start onboarding — flow selection (agreed concept)
The client supplies a flow `type` in its start-onboarding request. The exact endpoint/body shape remains to be specified. The engine resolves a configured flow for that type server-side.

## Async REST connector contract (conceptual; 2026-11-15)

1. Configured REST start call initiates work and returns a connector-generated polling ID.
2. Engine stores polling ID, marks the step `WAITING`, and polls the configured connector endpoint independently of the mobile app.
3. Final HTTP `200` supplies a successful result; explicit mapping controls internal state and optional response `additionalAttributes`. Final errors follow configured rejection vs technical retry rules.
4. **OPEN:** How is "still processing" represented in the polling response? `202 Accepted` is proposed but not agreed.


## Optional integration on a configured step (agreed 2026-11-16)
A step may have **no `integration` field** (e.g. ordinary data collection). When external processing is needed, company-internal flow configuration can use `integration: { type: "sync-rest" | "async-rest", mapping: {...}, connectivity: { url, path, method, headers, payload } }`. The complete input `schema` is defined separately and returned to the app; the `integration` object is never sent to the app.


## OTP example — latest mobile contract (2026-11-16)

- `initiate-otp`: `nextStep` is a `CHALLENGE` (not `INTERNAL`); the engine executes its configured OTP-delivery integration when the challenge is initiated, then proceeds to confirmation after a successful send.
- `confirm-otp`: `nextStep` is `USER_INPUT` with a complete JSON Schema for `{ "code": "1234" }`; submission is validated by its configured REST integration.
- Neither response exposes the internal integration URL or mappings. `session.jwt` and `additionalAttributes` remain part of the usual response envelope.
- The flow's transitions, not the client, choose the subsequent step.

Illustrative response after OTP delivery:

```json
{
  "session": { "jwt": "eyJ..." },
  "nextStep": {
    "id": "confirm-otp",
    "type": "USER_INPUT",
    "schema": {
      "type": "object",
      "properties": { "code": { "type": "string", "minLength": 4, "maxLength": 4 } },
      "required": ["code"]
    }
  },
  "additionalAttributes": {}
}
```

## Authoritative flow example

Use `../examples/flow.json` and `../examples/README.md`. When a step executes its optional `integration` is invoked; no trigger field is necessary. The resolver processes `INTERNAL` actions without returning an internal step to the mobile client. `transitions` are flow-level. `initiate-otp` is a `CHALLENGE`, `confirm-otp` is `USER_INPUT`. Exact connector response mapping and async pending status format are deferred.
