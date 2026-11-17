# Canonical example flow (conceptual)

`flow.json` is the single current reference example of our **agreed flow structure**, not a formal executable JSON Schema or a finished integration mapping DSL.

- Business order: phone (`USER_INPUT`) → CAPTCHA (`CHALLENGE`) → initiate OTP (`CHALLENGE`) → confirm OTP (`USER_INPUT`) → address (`USER_INPUT`) → risk (`INTERNAL` async REST) → completion or rejection.
- `steps` define input/challenge and optional `integration`; `transitions` are **separate at flow level** and may use CEL `when` expressions. `startStep` is explicit.
- Any configured integration is invoked **when its step is executed**. Initiating OTP sends SMS via REST; confirming OTP submits entered code via REST. The app only receives user-facing steps (`USER_INPUT`, `CHALLENGE`, `WAIT`, `COMPLETE_ONBOARDING`), never internal REST configuration.
- CAPTCHA is a first-class explicit challenge step, verified by the backend before completion. Ordering guarantees SMS cannot be reached before it; independent anti-abuse rate/spending controls remain required.
- `INTERNAL` risk is engine-executed, not shown to the app. Its async REST connector provides a polling ID; engine polls and app receives `WAIT` until the result. Exact pending HTTP representation remains deferred.
- Success (risk score >= 70): HTTP 200 response `nextStep.type = COMPLETE_ONBOARDING`; rejection: error payload without `nextStep`. Both are terminal **outcomes**, not steps.
- The `mapping`, `connectivity` placeholders, OTP initiation challenge payload, and exact response mapping rules remain illustrative. Their precise wire contracts will be determined during implementation, not architecture.
- `integration` is optional and omitted for pure data-collection steps; no extra `onEnter` or `onSubmit` trigger is required.
- Each step submission is persisted with `submission_version` integer, timestamp and data/outcome. Flow/schema versioning and database migrations are different concepts.

**Agreed to defer:** Exact connector response formats (including whether pending uses HTTP 202); error mapping; duplicate request protections; detailed implementation/persistence strategies.
