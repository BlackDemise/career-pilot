# Backend Architecture

This document describes the intended package structure of `be/` and the reasoning behind it. The
codebase currently only has `CareerPilotApplication.java`; this is the target structure to grow
into as features are implemented.

## Target Package Structure

```
blackdemise.cp
├── CareerPilotApplication.java
├── common/                 # shared base classes: BaseEntity, error codes, generic exceptions
├── config/                 # SecurityConfig, WebConfig, CORS, bean wiring
├── ai/                     # centralized Gemini integration (see below)
│   ├── GeminiClient.java
│   ├── AiService.java
│   └── prompt/             # prompt template loading, variable substitution, versioning
├── user/                   # User entity, profile/instructions, auth
├── chat/                   # Conversation, Message: controller/service/repository/dto/entity
├── cv/                     # CV, CVAnalysis: controller/service/repository/dto/entity
├── interview/              # InterviewSession, Question, Answer, Evaluation
└── security/               # authentication filters/config (basic auth for MVP)
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

## MVP Entities

Per [docs/06-roadmap-scope.md](./06-roadmap-scope.md) section 23:

- `User`
- `Conversation`, `Message`
- `Cv`, `CvAnalysis`
- `InterviewSession`, `InterviewQuestion`, `InterviewAnswer`, `InterviewEvaluation`

## Configuration

- All configuration lives in `application.yml` and is sourced from environment variables (see
  the existing `spring.datasource.*` pattern). The Gemini API key will follow the same pattern
  once AI integration begins — never committed, never hardcoded.
- `spring.jpa.hibernate.ddl-auto: update` is acceptable for local development only; a migration
  tool (e.g. Flyway) should be introduced before this project handles anything beyond local/dev
  data.

## Error Handling

A single `@ControllerAdvice` maps domain exceptions (not found, validation, AI failures) to the
consistent error response shape defined in
[api.instructions.md](../.github/instructions/api.instructions.md).
