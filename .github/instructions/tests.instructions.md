---
description: "Use when writing or modifying tests for the backend (be/src/test) or frontend (fe/**/*.test.*), or when defining test strategy."
applyTo: ["be/src/test/**", "fe/**/*.test.*", "fe/**/*.spec.*"]
---

# Testing Instructions

Full strategy is documented in
[docs/05-testing-strategies.md](../../docs/05-testing-strategies.md). Key rules:

- Backend: use JUnit 5 + Mockito. Name unit tests `*Test.java` and integration tests that boot a
  Spring context or hit a real database `*IT.java`.
- Backend: prefer Spring Boot test slices (`@WebMvcTest`, `@DataJpaTest`) over full
  `@SpringBootTest` where possible, to keep the suite fast.
- Backend: always mock the Gemini AI client in unit tests; never call the real API from
  automated tests.
- Frontend: use Vitest + React Testing Library (add to `fe/package.json` when frontend testing
  starts). Test observable behavior/output, not implementation details.
- Prioritize test coverage for: service-layer business logic, prompt-building/response-parsing
  logic, and the mock-interview state machine — these carry the most risk of silent breakage.
- Never write tests that depend on real network calls (Gemini, web search); use fakes/mocks.
