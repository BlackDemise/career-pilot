# Use Cases & Implementation Order

This document lists concrete use cases derived from the MVP scope in
[docs/06-roadmap-scope.md](./06-roadmap-scope.md), grouped by feature area, in the order they
should be implemented. Later phases (V1/V2/Advanced) are noted but not detailed here — see the
roadmap doc for their full description.

All API endpoints use the `ApiResponse` wrapper with `timestamp`, `statusCode`, `message`, and
`result`. Business errors set `result` to `null`; validation errors put field messages in
`result` as a `Map<String, String>`.

Priority legend: **P0** = required for MVP, **P1** = V1, **P2** = V2, **P3** = Advanced (not
planned unless explicitly requested).

## 0. Shared AI Infrastructure (build first — everything depends on this)

| # | Use Case | Priority | Completed? |
|---|----------|----------|------------|
| 0.1 | Centralized Gemini AI service (model config, temperature, timeout, error handling) | P0 | Yes |
| 0.2 | Prompt templates organized by workflow, loaded from resources (not hardcoded) | P0 | Yes |
| 0.3 | Persistence layer (PostgreSQL + JPA + Flyway migrations) with MVP entities (see [04-backend-architecture.md](./04-backend-architecture.md)) | P0 | Yes |
| 0.4 | Auth: register/login by email+password, stateless JWT (access token + httpOnly-cookie refresh token), Redis-backed revocation | P0 | Yes |
| 0.5 | Streaming responses, token usage tracking, retry with limits | P1 | No |

### Authentication Details

- **Registration**: `POST /api/v1/auth/register` validates the required fields and stores a
	pending registration in Redis. It sends a frontend link valid for one hour. The account is
	created only when the link is verified through `POST /api/v1/auth/register/verify`; successful
	verification redirects the user to login rather than issuing tokens automatically.
- **Registration resend**: `POST /api/v1/auth/register/resend` is available from the pending
	registration page only. A new email may be requested after a 60-second cooldown. The latest
	link invalidates the previous link and remains valid for one hour.
- **Forgot password**: `POST /api/v1/auth/forgot-password` sends a one-hour frontend reset link
	when the email belongs to an account and always returns the same generic response otherwise.
- **Password reset resend**: `POST /api/v1/auth/forgot-password/resend` is available from the
	pending reset page only, after the same 60-second cooldown; the latest link invalidates the
	previous link.
- **Password reset**: `POST /api/v1/auth/reset-password` requires a valid one-time link token,
	a matching password confirmation, and the shared password complexity rule. Existing access
	tokens are not revoked immediately; they expire within their normal 15-minute maximum.
- **Password rule**: passwords require at least 8 characters, one uppercase letter, one
	lowercase letter, one digit, and one special character.

## 1. AI Chat

| # | Use Case | Priority | Completed? |
|---|----------|----------|------------|
| 1.1 | User creates a new conversation | P0 | Yes |
| 1.2 | User sends a message and receives an AI response | P0 | Yes |
| 1.3 | Conversation history is saved and can be reloaded | P0 | Yes |
| 1.4 | User sets instructions/profile (language, style, background, career goal) applied to all conversations | P0 | Yes |
| 1.5 | Chat enforces career/tech scope; out-of-scope questions are refused via system prompt | P0 | Yes |
| 1.6 | User deletes a conversation | P0 | Yes |
| 1.7 | Streaming responses, regenerate/retry, edit user message, markdown rendering | P1 | No |
| 1.8 | Intent classification (GENERAL_CAREER, TECHNICAL, CV_DISCUSSION, INTERVIEW_DISCUSSION, OUT_OF_SCOPE) | P1 | No |
| 1.9 | Conversation summarization for long conversations | P1 | No |
| 1.10 | Web search grounding, semantic/long-term memory, cross-conversation retrieval | P2/P3 | No |

## 2. CV Analysis

| # | Use Case | Priority | Completed? |
|---|----------|----------|------------|
| 2.1 | User uploads a PDF CV | P0 | Yes |
| 2.2 | System extracts text from the CV | P0 | Yes |
| 2.3 | User requests a CV Review; system returns overall assessment, strengths, weaknesses, recommendations | P0 | Yes |
| 2.4 | User submits a CV + JD pair; system returns match score, matched/missing skills, experience gaps, recommendations | P0 | Yes |
| 2.5 | DOCX/TXT support, structured CV extraction (education/experience/projects/skills sections) | P1 | No |
| 2.6 | Section-level scoring, requirement categorization (required/preferred/optional) | P1 | No |
| 2.7 | Evidence mapping (JD requirement → CV proof, with confidence), ATS-oriented analysis | P2 | No |
| 2.8 | CV rewrite suggestions, automatic CV tailoring | P3 | No |

## 3. Mock Interview

| # | Use Case | Priority | Completed? |
|---|----------|----------|------------|
| 3.1 | User configures an interview (role, level, topic, number of questions, difficulty) | P0 | No |
| 3.2 | System generates questions based on the configuration | P0 | No |
| 3.3 | User submits an answer to a question | P0 | No |
| 3.4 | System evaluates the answer (score, strengths, weaknesses, feedback) | P0 | No |
| 3.5 | Interview progresses through a state machine (SETUP → QUESTION → ANSWER → EVALUATION → ... → COMPLETED) | P0 | No |
| 3.6 | System produces a final report (overall score, strengths, weaknesses, recommendations) | P0 | No |
| 3.7 | Structured questions/evaluation, adaptive difficulty, follow-up questions, JD-based interview config | P1 | No |
| 3.8 | Web search for version-specific/fresh knowledge (e.g. current framework versions) | P1 | No |
| 3.9 | Fully adaptive interviewer, resume-based interview, performance tracking across sessions | P2/P3 | No |

## 4. Cross-Feature Integration (not MVP)

| # | Use Case | Priority | Completed? |
|---|----------|----------|------------|
| 4.1 | Discuss CV analysis results inside chat | P1 | No |
| 4.2 | Discuss interview results inside chat | P1 | No |
| 4.3 | Generate interview configuration from CV or JD | P2 | No |

## Recommended Build Order

1. Shared AI infrastructure (0.1–0.4)
2. Basic Chat (1.1–1.6) — validates the AI service end-to-end with the simplest workflow
3. CV Review + CV/JD Analysis (2.1–2.4) — validates document handling and structured output
4. Mock Interview (3.1–3.6) — validates stateful, multi-turn AI workflows

This order matches [docs/06-roadmap-scope.md](./06-roadmap-scope.md) section 29: prove three
workflows of a genuinely different nature on top of one well-designed AI infrastructure, rather
than building every capability for one workflow first.
