# MVP acceptance walkthrough (after implementation)

1. Start flow by type and receive phone form.
2. Submit phone, receive CAPTCHA challenge, verify it server-side.
3. Invoke OTP initiation, confirm SMS connector called exactly under the configured security controls.
4. Receive OTP code schema, submit code; wrong code follows attempt policy, correct code advances.
5. Observe final `COMPLETE_ONBOARDING` response with refreshed JWT.
6. Verify submission history, resumption, expiry and rejection behavior.

Use `examples/flow.json` for a **broader** reference with address and async risk, not as the exact four-step MVP acceptance fixture.
