# Conceptual Data Model (NOT database schema)

**Status:** Preliminary. Entity names and fields are subject to design review.

| Entity | Candidate fields / role |
|---|---|
| `FlowDefinition` | `flowId`, `version`, `schemaVersion`, immutable document, activation policy |
| `StepDefinition` | `stepId` (business-defined screen or system activity), `inputSchema` (one or many fields), `outputSchema`, optional `action` (none / REST / async, including on-submit action), explicit mappings, forward-only transitions, predicates |
| `OnboardingSession` | `onboardingId` (authoritative JWT claim and DB lookup key), flow version ref, lifecycle status, current step, timestamps, authorization reference |
| `StepExecution` | session, step, `submission_version` (integer attempt counter), timestamp, status, input/output references, started/completed times |
| `PendingAction` | `onboardingId`, step/action reference, deadline, dispatch status, external result status |
| `TransitionRecord` | from/to steps, reason, version, timestamp, sequence number |
| `OutboxMessage` | message ID, type, correlation, dispatch status, retry metadata (if outbox adopted) |

A step is not synonymous with a field; `WAITING` represents execution state rather than a mandatory declared step type. V1 flow edges go forward only (conditional branching allowed; no cycles or nested subflows).

A `verify-phone` StepExecution accepts a code submitted through the onboarding API. The engine validates via a configured REST provider, mapping the code and previously stored phone number. Invalid codes may keep the step actionable with remaining attempts. Provider output must be explicitly mapped to stored attributes and mobile-safe response data.

## Tentative state machine

`CREATED` -> `ACTION_REQUIRED` / `EXECUTING` -> `WAITING` (async) -> `EXECUTING` / `ACTION_REQUIRED` -> `COMPLETED` or `REJECTED`; technical faults may enter `FAILED` / retryable states. This is **an example**, not an approved finite state machine.

## Data handling constraints

- Sensitive input/output may require encrypted blobs and limited-access references, not unrestricted event payloads.
- Bind every input/output to a session, flow version, step `submission_version`, and authorization context.
- Model `businessRejected` distinctly from `technicalFailed`.
- `onboardingId` correlates a journey but does not authenticate event producers or uniquely identify multiple pending operations; associate the waiting step/action and verify the event source.
- Define deletion behavior for both stored PII and outstanding asynchronous work.

## Open issues

How many simultaneous pending actions? Parallel step execution? Deterministic ordering? How to mark downstream results stale after edits? Data residency? What must be evented vs stored?

## Historical edits and invalidation

Each resubmission creates a new step record with an incrementing integer `submission_version`, timestamp, user input, and outcome. The latest valid attempt is effective; older attempts are retained. A resubmission of an earlier step marks every subsequent step execution/result invalid for the current path. Forward progression can then recompute downstream work. Introduce a conceptual **point-of-no-return** constraint for irreversible steps; representation and persistence are TBD.

## Multiple flow types (agreed)
`FlowDefinition` includes an industry-agnostic `type` (e.g., `RETAIL_SIGNUP`, `PERSONAL_BANKING`), alongside `flowId` and `version`. The client app supplies this type at onboarding start; the server resolves and validates the allowed flow. Multiple flow types can be active concurrently. `type` does not replace unique flow ID or version.
