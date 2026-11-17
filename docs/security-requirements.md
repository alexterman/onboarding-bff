# Security Requirements — Public Endpoints & OTP Abuse

**Status:** Requirements baseline (2026-11-15). Default CAPTCHA provider selected; quotas and exact provider-specific configuration remain OPEN.

## Threat to address

An attacker can repeatedly create **new onboarding sessions using new IPs** and trigger thousands of billable SMS OTP requests. Session- and IP-only controls are inadequate. The first SMS in every session is attackable.

## Mandatory controls

1. **Admission before expense.** Before a billable SMS/KYC action or expensive durable workflow is started, enforce server-side abuse checks. For SMS OTP, the **initial default is an explicit CAPTCHA step before every send**, including the first, even from a new session/IP. The flow resolver validates its completion in persisted step history. A future policy MAY invoke CAPTCHA conditionally; this is optimization, not a relaxation of default protection. Validate tokens on the server and prevent replay.
2. **Overlapping limits.** Rate limit and quota by normalized destination phone number, session, IP/network, device signals where available, and global/provider budget. Use rolling windows or equivalent protections against boundary bursts. Do not count on any single dimension. Make limits configurable; example thresholds from external feedback (3–5 sends/hour/phone, etc.) are **not approved defaults**.
3. **Resend friction without denial-of-service lockout.** Cooldowns and progressive throttling apply to resends. Avoid automatic 1–24-hour destination lockouts, which attackers can exploit to prevent a real customer from onboarding.
4. **OTP correctness.** Generate server-side only after checks; use cryptographically secure randomness, short expiry (proposed 5–10 minutes; configurable), single use, and non-plaintext verifier storage. Bound wrong-code attempts per challenge/destination/session and use constant-time comparison where applicable. Sending and verifying have distinct counters.
5. **Budget and provider controls.** Global/provider send limits, spend alerts and circuit breakers, and provider-side country/destination restrictions or fraud protection when available. Refuse sends if a mandatory limit/check cannot be reliably enforced.
6. **Public API/session protection.** Short-lived, unguessable, scoped session credentials; verify authorization on every call. Gateway/WAF, request-size limits, strict schema/transition validation, and protection against repeated creation of costly pending workflows.
7. **Observability and privacy.** Audit decisions, challenge outcomes, send/verify counts, provider errors, anomalies, and budget breaches with pseudonymized identifiers and minimal retention. Generic outward responses when needed to avoid account enumeration; alert operators on attacks.
8. **Honeypot form fields.** Support server-recognized, non-visible trap fields in public onboarding forms. A populated trap field MUST trigger rejection or an abuse response without advancing the session or calling external providers. Do not rely on honeypots alone: some assistive technology or password managers may populate hidden inputs; design fields to avoid accessibility and autofill false positives. Never disclose which field triggered detection.
9. **Minimum step interaction time.** Record a trusted server-side time when each eligible user-facing step becomes available. If a submission arrives sooner than a configurable step-specific threshold, reject or challenge it without initiating billable work. Client timestamps are not trusted; account for legitimate fast users and retries.
10. **Test the actual attack.** Simulate 6,000 first-send attempts using fresh sessions and rotating IPs for one number; ensure bot verification and per-destination/global budgets stop cost amplification. Also test distributed destination floods, replay, concurrent bursts, wrong OTP guessing, and provider failures.

## CAPTCHA provider decision (2026-11-15)

- **Initial reference/default integration: Cloudflare Turnstile** for human/bot verification on public onboarding requests. This is a provider choice, **not** a requirement to host the application on Cloudflare.
- Define a **replaceable CAPTCHA provider interface** so adopting organizations can use Turnstile, Google reCAPTCHA, hCaptcha, or their own service without changing the flow engine.
- The client obtains a challenge token; the server validates it with the configured provider **before** any SMS or other protected billable work. Required verification failures must fail closed.
- **Initial policy: challenge every OTP send, including first sends.** Future conditional/risk-based invocation is optional and must be explicitly configured, not assumed.
- Valid CAPTCHA does **not** override destination/global limits, spending caps, or other abuse controls.
- Avoid depending on a specific vendor pricing/free-tier claim in the core specification, as commercial terms can change.

## CAPTCHA provider flexibility

Support image selection challenges, checkbox challenges, and passive/invisible bot verification through provider adapters. These are different user experiences of the same anti-automation purpose; all required challenges MUST be verified server-side. For now, CAPTCHA remains mandatory before OTP SMS by default, regardless of provider choice.

## Out of scope / future options

- Risk-adaptive CAPTCHA invocation, passive risk scoring, attestation, email magic links, or stronger identity factors are **possible enhancements**, not substitutes for protecting the first SMS.
- Product-specific settings, exact rate limits, provider-specific credentials and settings, Redis versus database backing, SMS provider, and fallback channel are **OPEN**.
- A phone OTP verifies access to a number; it is **not** full identity verification.

## Email verification protection (2026-11-16)

The same pre-send CAPTCHA/bot verification gate applies to email OTP/verification dispatch, as well as SMS dispatch. It is enforced server-side before invoking the configured delivery provider. Existing rate limits, anti-replay and cost controls continue to apply per appropriate destination/channel.

## Decision — CAPTCHA as an explicit flow step (2026-11-16)

- CAPTCHA is configured as a normal step in the JSON journey. The sole flow resolver presents that step and enforces its sequence using recorded successful completions. No independent security layer inserts a separate CAPTCHA `nextStep`.
- The backend validates the Turnstile/provider token as part of completing the CAPTCHA step, then records its successful completion. Provider-token expiry afterward does not invalidate completion.
- If CAPTCHA must be performed more than once, specify distinct CAPTCHA steps for each occurrence. For SMS/email example flows, place the relevant CAPTCHA step before each verification-message send.
- **Defense in depth is still mandatory:** rate limits, destination quotas, provider spending limits, and circuit breakers remain independent controls; a completed CAPTCHA does not bypass them.
