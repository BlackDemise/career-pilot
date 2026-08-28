# Backend Architecture

This document describes the package structure of `be/` and the reasoning behind it.

## Package Structure

```
blackdemise.cp
├── CareerPilotApplication.java
├── common/                 # BaseEntity, ApiResponse, exception/ (ApiException + subtypes),
│                           # GlobalExceptionHandler
├── config/                 # SecurityConfig
├── ai/                     # centralized Gemini integration (see below)
│   ├── GeminiClient.java
│   ├── AiService.java
│   └── prompt/             # prompt template loading, variable substitution
├── user/                   # User entity, Role enum, AuthService/AuthController, auth DTOs
├── chat/                   # Conversation, Message: controller/service/repository/dto/entity
├── cv/                     # CV, CVAnalysis: controller/service/repository/dto/entity
├── interview/              # catalog, session state machine, WebSocket interviewer, report
└── security/                # RestAuthenticationEntryPoint, RestAccessDeniedHandler
    └── jwt/                 # JwtProperties, JwtTokenProvider, TokenBlacklistService (Redis),
                             # JwtAuthenticationFilter, JwtUserPrincipal, TokenType
```

Each feature package (`chat`, `cv`, `interview`, `user`) is internally organized by layer:

```
chat/
├── ChatController.java
├── ChatService.java
├── ConversationRepository.java
├── MessageRepository.java
├── dto/
│   ├── ConversationResponse.java
│   └── SendMessageRequest.java
└── entity/
    ├── Conversation.java
    └── Message.java
```

The mock interview workflow is described in detail in
[docs/04-1-backend-interview.md](./04-1-backend-interview.md). It is a turn-by-turn AI interviewer,
not a batch question generator. HTTP is used for catalogs, session creation, session retrieval, and
the final report. A WebSocket carries interviewer turns, answer submission, timeout notifications,
integrity events, and lifecycle events. The backend remains authoritative for ownership, state,
phase transitions, duration, question budgets, and early termination.

## Reasoning

- **Feature packages over layer-only packages**: keeps everything about "Chat" (or CV, or
  Interview) together, matching the three workflows in
  [docs/02-use-cases.md](./02-use-cases.md), and avoids a single `controller/` package with
  every controller in the system.
- **Dedicated `ai` package**: Gemini access, model configuration, and prompt handling are used
  by all three features, so they must live in one shared, testable module rather than being
  duplicated or called directly from feature services. This is what
  [backend.instructions.md](../.github/instructions/backend.instructions.md) enforces.
- **Strict layering within a feature**: Controller (HTTP concerns, validation) → Service
  (business logic, orchestrates `ai` calls) → Repository (persistence only). DTOs are the only
  types that cross the controller boundary; entities never leave the service/repository layer.
- **Catalog-driven interview configuration**: roles, levels, topics, compatibility, required topics,
  and topic selection weights are database-backed. The backend rejects unknown or incompatible
  values before a session begins; Gemini never validates user configuration.
- **Live interview orchestration**: Gemini may propose a next question, follow-up, phase transition,
  or early-ending recommendation, but the backend applies deterministic policy before accepting it.
  Candidate-facing evaluation is withheld until the interview reaches `COMPLETED`.

## MVP Entities

Per [docs/06-roadmap-scope.md](./06-roadmap-scope.md) section 23:

- `User` (`firstName`, `lastName`, `email` unique, `password`, `role`, preferred language,
  response style, technical background, career goal, custom instructions)
- `Conversation`, `Message`
- `Cv`, `CvAnalysis`
- `InterviewRole`, `InterviewLevel`, `InterviewTopic`, and compatibility/plan records
- `InterviewSession`, `InterviewQuestion`, `InterviewAnswer`, `InterviewEvaluation`, and integrity events

## Configuration

- All configuration lives in `application.yml` and is sourced from environment variables (see
  the existing `spring.datasource.*` pattern) — never committed, never hardcoded.
- `spring.jpa.hibernate.ddl-auto: validate` — the schema is owned entirely by Flyway migrations
  under `be/src/main/resources/db/migration`, not by Hibernate auto-DDL. There is no code-based
  data seeding; the one seed account (an initial `ADMIN`) is inserted by a Flyway migration using
  placeholders bound to `SEED_ADMIN_*` env vars.

## Authentication

- Stateless JWT: a 15-minute access token (returned in the `/api/v1/auth/*` JSON response body,
  read by the frontend from `Authorization: Bearer`) and a 7-day refresh token (set by the
  backend as an httpOnly cookie, scoped to the `/api/v1/auth` path — never exposed in a response
  body or to JavaScript).
- `JwtAuthenticationFilter` authenticates purely from the access token's own claims (`sub`,
  `email`, `firstName`, `lastName`, `role`) — no DB lookup per request — and rejects tokens that
  are the wrong type, or blacklisted.
- `TokenBlacklistService` revokes a token by its `jti` in Redis, with a TTL equal to the token's
  remaining lifetime, so entries expire on their own (no scheduled cleanup job). `/refresh`
  rotates (blacklists) the presented refresh token immediately, even on success.
- `Role` is `USER` (default, all self-registered accounts) or `ADMIN` (reserved for future
  administrative use; not yet wired to any endpoint restriction).
- Registration and password recovery use one-time Redis tokens with a one-hour TTL. Raw tokens
  are sent only in links to frontend routes; Redis stores token hashes and pending data. Resend
  requests are limited by a 60-second cooldown and invalidate the previous active link.
- `POST /api/v1/auth/register` does not create a user immediately. Verification creates the
  `USER` account and returns a success message for frontend redirection to login. Forgot-password
  requests always return a generic response, including for unknown email addresses.

## Error Handling

A single `@RestControllerAdvice` (`common.GlobalExceptionHandler`) maps `common.exception.*`
domain exceptions and validation failures to the universal `ApiResponse` shape defined in
[api.instructions.md](../.github/instructions/api.instructions.md); `RestAuthenticationEntryPoint`
/ `RestAccessDeniedHandler` handle the 401/403 cases raised by Spring Security itself (before a
controller is even reached) in the same shape.
