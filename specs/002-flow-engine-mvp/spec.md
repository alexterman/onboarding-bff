# Feature 002 — Flow engine MVP

**Status:** Planned after scaffold.

## Goal

Run the first valuable configurable onboarding journey, end to end, through the same generic mobile API:

`Phone (USER_INPUT) → CAPTCHA (CHALLENGE) → Initiate OTP (CHALLENGE) → Confirm OTP (USER_INPUT) → COMPLETE_ONBOARDING`.

## Acceptance scenarios

1. Load a versioned JSON flow at startup, validate its `startStep`, step schemas and separately defined transitions. One active version per flow type in v1; redeploy to change flow.
2. Phone input is stored and later steps may access only explicitly mapped data.
3. The CAPTCHA step supplies provider/public config **from its flow JSON step definition** to the client and validates the response with default provider Cloudflare Turnstile; protected verification credentials are obtained from secret references, never sent to the app. CAPTCHA precedes sending any SMS.
4. When `initiate-otp` executes, its optional `sync-rest` integration sends an SMS, and the engine returns the OTP confirmation form. Integration execution occurs when its step executes.
5. On `confirm-otp`, invoke configured REST validation, enforce configured attempt policy and advance on valid input.
6. Persist session state and every submitted step attempt with integer `submission_version`, timestamp, data and result. Resume by validated JWT `onboardingId` when an unexpired session exists.
7. Return a new signed session JWT on each successful call; only completed steps renew expiry. Client polling does not renew expiry; expired data is deleted.
8. Success returns `nextStep.type: COMPLETE_ONBOARDING`; business rejection returns a separate error payload. No internal step is surfaced as `nextStep`.
9. Preserve the architecture for future async REST steps (`WAIT`), backend polling, CEL transitions and later provisioning without requiring that entire feature in the initial OTP slice.

## Security

Apply the agreed [security requirements](../../docs/security-requirements.md), including per-destination/global rate limits and provider budgets alongside explicit CAPTCHA steps.

## Out of initial slice

The **larger canonical reference flow** also contains address collection and async risk assessment; these are documented for subsequent slices under `examples/flow.json`. Detailed connector response mappings, async pending HTTP status, scheduling and point-of-no-return design are deferred.

## References

- [Product-wide requirements](../../docs/product-requirements.md)
- [Architecture](../../docs/architecture.md)
- [Decision log](../../docs/decisions.md)
