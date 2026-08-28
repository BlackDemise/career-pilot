# Testing Strategies

This document describes how both `be/` and `fe/` should be tested. See
[tests.instructions.md](../.github/instructions/tests.instructions.md) for the enforced rules
applied while writing tests.

## Backend (`be/`)

- **Frameworks**: JUnit 5, Mockito, Spring Boot Test (already on the classpath via
  `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`,
  `spring-boot-starter-webmvc-test` in `be/pom.xml`).
- Every controller and security error response must use `ApiResponse`; tests should assert its
  timestamp, numeric statusCode, message, and result shape.
- **Naming convention**: unit tests end in `*Test.java`; integration tests that boot a Spring
  context or touch a real database end in `*IT.java`.
- **Test types**:
  - **Unit tests** for services: mock repositories and the `ai` package's `AiService`/
    `GeminiClient`; verify business logic (e.g. prompt variable assembly, response parsing,
    interview state transitions) without any network or DB dependency.
  - **Slice tests** (`@WebMvcTest`) for controllers: verify request validation, status codes,
    and error-shape mapping, with the service layer mocked.
  - **Slice tests** (`@DataJpaTest`) for repositories: verify queries against an in-memory or
    Testcontainers-backed PostgreSQL instance.
  - **Integration tests** (`*IT.java`, full `@SpringBootTest`) reserved for a small number of
    end-to-end flows (e.g. create conversation → send message → response persisted).
- **Never call the real Gemini API in tests.** Always mock `AiService`/`GeminiClient` at the
  boundary and assert on how it was called and how its response is used.

## Frontend (`fe/`)

- **Frameworks (to add when frontend testing starts)**: Vitest (pairs naturally with Vite) and
  React Testing Library. Not yet present in `fe/package.json` — add them as a dedicated setup
  step before the first test is written.
- **What to test**:
  - Feature hooks and API-layer functions (`features/*/api.ts`) with the HTTP client mocked.
  - Component behavior via React Testing Library (render, user interaction, assertions on
    rendered output) rather than internal state/implementation details.
  - Avoid testing third-party library internals or trivial prop pass-through components.

## Coverage Priorities (highest risk first)

1. AI prompt-building and response-parsing logic (backend `ai` package and feature services).
2. Mock interview state machine transitions.
3. CV/JD structured-output mapping (match score, missing skills, etc.).
4. Controller-level validation and error-shape consistency.
5. Frontend feature hooks/components for Chat, CV Analysis, Mock Interview screens.

## Email Authentication Tests

- Registration must not create a `User` before a valid verification token is consumed.
- Verification tokens and password-reset tokens must expire after one hour, be single-use, and
  invalidate the previous link when a resend is sent.
- Registration and reset requests must enforce a 60-second resend cooldown.
- Forgot-password requests must return the same public response for known and unknown emails and
  must not send mail for an unknown email.
- Password tests must cover the eight-character minimum and uppercase, lowercase, digit, and
  special-character requirements, as well as password confirmation mismatch.
- Mock `JavaMailSender` and Redis in unit tests; never send real email or call a real Redis server
  from the unit suite.

## Out of Scope for Now

- End-to-end browser tests (e.g. Playwright) and CI pipeline wiring are not part of MVP; revisit
  once the three core workflows are functionally complete.
