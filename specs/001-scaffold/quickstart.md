# Scaffold quickstart (to be completed by T012)

0. From `onboarding-bff/`, instruct Claude to implement T001 from `onboarding-engine-sdd/specs/001-scaffold/tasks.md`; after Claude verifies the copied root-level specs and scaffold, delete the original `onboarding-engine-sdd/` folder.
1. Install a compatible JDK, Docker and PostgreSQL (or run PostgreSQL using Docker).
2. Configure local environment variables using the nonsecret template created in T003; application settings live in `src/main/resources/application.yml`.
3. Run Liquibase migrations through the Quarkus application configuration.
4. Start the app via `./gradlew quarkusDev` and test with `./gradlew test`.
5. Check health, OpenAPI placeholder endpoints and CI. These commands are targets for the implementation; no application source code exists yet.
