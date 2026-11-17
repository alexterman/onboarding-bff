# Tasks — Feature 002: Flow engine MVP

**Status:** Preliminary; refine after feature 001 scaffold is implemented and its API contract is validated.

- [ ] **M001** Validate and load startup flow JSON, including unique IDs, step types, schemas and forward-only transitions.
- [ ] **M002** Store an onboarding session and resolve the next allowed step from validated JWT and persisted history.
- [ ] **M003** Implement generic step submission and per-step JSON Schema validation.
- [ ] **M004** Record each user submission attempt with `submission_version`, timestamp, input and result.
- [ ] **M005** Implement `USER_INPUT` and `CHALLENGE` step handlers; optional integration execution when invoked.
- [ ] **M006** Integrate Turnstile CAPTCHA verification and return public provider configuration to mobile.
- [ ] **M007** Add configurable sync REST connector for OTP initiation and confirmation, including safe mappings and `X-Onboarding-ID`.
- [ ] **M008** Enforce CAPTCHA-before-SMS flow progression plus destination/global rate limits and cost controls.
- [ ] **M009** Implement configurable retry/attempt behavior for OTP validation and consistent error envelopes.
- [ ] **M010** Implement bounded sliding session JWT refresh, expiry and secure data cleanup.
- [ ] **M011** Implement terminal completion and failure responses plus end-to-end REST Assured/Testcontainers tests.

## Subsequent slice (not committed to this first MVP)

Address collection, async REST connector polling with durable WAIT, CEL-based risk branching, irreversible-step policy, visualization and live editing.
