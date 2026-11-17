# Implementation plan — 002 Flow engine MVP

**Status:** Provisional: begin after feature 001 acceptance.

## Design

- Kotlin/Quarkus single service, PostgreSQL/JSONB via JDBC, Liquibase and generic OpenAPI API from feature 001.
- Load flow JSON at startup, validate schema, ensure unique step IDs and resolvable forward-only transitions.
- Flow controller uses persisted history to determine next active step; step-specific JSON Schema validates `userInput`.
- Step handlers: `USER_INPUT` and `CHALLENGE`; integrations are optional per step and run when the step executes.
- External REST integration configuration is **exclusively per step in versioned flow JSON**: optional `integration` with `type`, `mapping` and `connectivity` (URL/path/method/headers/payload). **No named OTP, risk, async-connector, or CAPTCHA connector configuration in `application.yml`.** Protected credentials are resolved via secret references; exact mapping grammar is an implementation choice.
- JWT identifies `onboardingId`; store every submission attempt with `submission_version`, timestamp and outcome, and manage expiration/deletion.
- Cloudflare Turnstile default challenge; OTP send/verification via configurable REST connectors, with multi-layer abuse/cost controls.
- Transition routing lives separately from steps; use CEL for conditional transitions when they are added.

## Build sequence

1. Flow loader and strict structural validation.
2. Session persistence, JWT and step history.
3. Input-schema validation and transition resolver.
4. CAPTCHA challenge integration, OTP initiation and confirmation.
5. API responses and error policies, security tests and end-to-end acceptance.
6. Later: address, async REST risk polling, CEL branching, cleanup worker and future enhancements.

**Open choices:** exact connector payload/result mappings, pending status conventions, cleanup/polling scheduler, irreversible-step boundaries.
