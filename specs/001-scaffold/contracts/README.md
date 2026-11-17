# API-first scaffold contract

Define OpenAPI in feature 001 before implementing the endpoints.

- Start onboarding: app selects flow `type`; response returns session JWT and next step.
- Submit a step: `{ "stepId": "...", "session": {"jwt":"..."}, "userInput": {}, "additionalAttributes": {} }`.
- Next step / submit success: `{ "session": {"jwt":"..."}, "nextStep": {}, "additionalAttributes": {} }`.
- Failure: standalone `error` object, without `nextStep`.
- Use generic JSON objects for `userInput`, `additionalAttributes`, and dynamic `nextStep.schema`.

See [feature 002 contract examples](../../002-flow-engine-mvp/contracts/README.md); endpoint details are defined during implementation.
