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
- Use the official Google Gen AI Java SDK (`com.google.genai:google-genai`, see `be/pom.xml`) for
  all Gemini calls via `GeminiClient`; do not hand-roll REST calls to the Gemini API.
- Store prompt templates as resources organized by workflow (`chat-system`, `cv-review`,
  `cv-jd-analysis`, `interview-question`, `interview-evaluation`, ...) — never as inline string
  literals scattered through Java code.
- Use `@ControllerAdvice` / `@ExceptionHandler` for centralized error handling with a consistent
  error response shape (see [api.instructions.md](./api.instructions.md)); domain errors should
  extend `common.exception.ApiException` rather than throwing raw `RuntimeException`.
- Configuration lives in `application.yml`, sourced from environment variables (see
  `be/src/main/resources/application.yml`). Never hardcode secrets or API keys.
- Prefer Java 21 features (records for immutable DTOs, pattern matching, switch expressions)
  where they simplify code.
- Persistence is PostgreSQL via Spring Data JPA with `ddl-auto: validate` — schema changes are
  made exclusively through Flyway migrations under `be/src/main/resources/db/migration`, never
  by hand-editing the running database or relying on Hibernate to auto-generate DDL.
- Auth is stateless JWT (access token, 15m TTL; refresh token, 7d TTL, httpOnly cookie), with
  revoked/rotated tokens blacklisted in Redis by `jti` — never in the relational database, and
  never with a manual cleanup job (the blacklist entry's TTL matches the token's remaining
  lifetime, so Redis expires it automatically). See `security/jwt/`.
- Do not add any code-based data seeding (`CommandLineRunner`, etc.) for users or other domain
  data — seed data belongs in a Flyway migration.
