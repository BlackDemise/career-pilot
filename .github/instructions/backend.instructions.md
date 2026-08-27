---
description: "Use when writing or modifying Spring Boot backend code in be/: controllers, services, repositories, entities, DTOs, AI/Gemini integration, or configuration."
applyTo: "be/**"
---

# Backend Instructions (Spring Boot)

- Package root is `blackdemise.cp`. Organize by feature package (`chat`, `cv`, `interview`,
  `user`), each containing its own controller/service/repository/dto/entity, rather than
  grouping globally by layer. See [docs/04-backend-architecture.md](../../docs/04-backend-architecture.md).
- Follow strict layering: Controller → Service → Repository. Controllers must not access
  repositories directly, and must not contain business logic.
- Use constructor injection (Lombok `@RequiredArgsConstructor`) — never field-level `@Autowired`.
- Keep DTOs and JPA entities separate. Never return entities directly from controllers; map to
  DTOs explicitly or with a dedicated mapper.
- Validate all inbound DTOs with `jakarta.validation` annotations, enforced via `@Valid` at the
  controller boundary.
- Centralize all Gemini/AI calls behind a single service in an `ai` package. Feature services
  (chat, cv, interview) must call that shared service, never the Gemini API directly.
- Store prompt templates as resources organized by workflow (`chat-system`, `cv-review`,
  `cv-jd-analysis`, `interview-question`, `interview-evaluation`, ...) — never as inline string
  literals scattered through Java code.
- Use `@ControllerAdvice` / `@ExceptionHandler` for centralized error handling with a consistent
  error response shape (see [api.instructions.md](./api.instructions.md)).
- Configuration lives in `application.yml`, sourced from environment variables (see
  `be/src/main/resources/application.yml`). Never hardcode secrets or API keys.
- Prefer Java 21 features (records for immutable DTOs, pattern matching, switch expressions)
  where they simplify code.
- Persistence is PostgreSQL via Spring Data JPA. Do not rely on `ddl-auto: update` beyond local
  development; introduce a migration tool (e.g. Flyway) before anything resembling production use.
