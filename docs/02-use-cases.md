# Use Cases & Implementation Order

This document lists concrete use cases derived from the MVP scope in
[docs/06-roadmap-scope.md](./06-roadmap-scope.md), grouped by feature area, in the order they
should be implemented. Later phases (V1/V2/Advanced) are noted but not detailed here — see the
roadmap doc for their full description.

Priority legend: **P0** = required for MVP, **P1** = V1, **P2** = V2, **P3** = Advanced (not
planned unless explicitly requested).

## 0. Shared AI Infrastructure (build first — everything depends on this)

| # | Use Case | Priority |
|---|----------|----------|
| 0.1 | Centralized Gemini AI service (model config, temperature, timeout, error handling) | P0 |
| 0.2 | Prompt templates organized by workflow, loaded from resources (not hardcoded) | P0 |
| 0.3 | Persistence layer (PostgreSQL + JPA) with MVP entities (see [04-backend-architecture.md](./04-backend-architecture.md)) | P0 |
| 0.4 | Basic auth / single dev user | P0 |
| 0.5 | Streaming responses, token usage tracking, retry with limits | P1 |

## 1. AI Chat

| # | Use Case | Priority |
|---|----------|----------|
| 1.1 | User creates a new conversation | P0 |
| 1.2 | User sends a message and receives an AI response | P0 |
| 1.3 | Conversation history is saved and can be reloaded | P0 |
| 1.4 | User sets instructions/profile (language, style, background, career goal) applied to all conversations | P0 |
| 1.5 | Chat enforces career/tech scope; out-of-scope questions are refused via system prompt | P0 |
| 1.6 | User deletes a conversation | P0 |
| 1.7 | Streaming responses, regenerate/retry, edit user message, markdown rendering | P1 |
| 1.8 | Intent classification (GENERAL_CAREER, TECHNICAL, CV_DISCUSSION, INTERVIEW_DISCUSSION, OUT_OF_SCOPE) | P1 |
| 1.9 | Conversation summarization for long conversations | P1 |
| 1.10 | Web search grounding, semantic/long-term memory, cross-conversation retrieval | P2/P3 |

## 2. CV Analysis

| # | Use Case | Priority |
|---|----------|----------|
| 2.1 | User uploads a PDF CV | P0 |
| 2.2 | System extracts text from the CV | P0 |
| 2.3 | User requests a CV Review; system returns overall assessment, strengths, weaknesses, recommendations | P0 |
| 2.4 | User submits a CV + JD pair; system returns match score, matched/missing skills, experience gaps, recommendations | P0 |
| 2.5 | DOCX/TXT support, structured CV extraction (education/experience/projects/skills sections) | P1 |
| 2.6 | Section-level scoring, requirement categorization (required/preferred/optional) | P1 |
| 2.7 | Evidence mapping (JD requirement → CV proof, with confidence), ATS-oriented analysis | P2 |
| 2.8 | CV rewrite suggestions, automatic CV tailoring | P3 |

## 3. Mock Interview

| # | Use Case | Priority |
|---|----------|----------|
| 3.1 | User configures an interview (role, level, topic, number of questions, difficulty) | P0 |
| 3.2 | System generates questions based on the configuration | P0 |
| 3.3 | User submits an answer to a question | P0 |
| 3.4 | System evaluates the answer (score, strengths, weaknesses, feedback) | P0 |
| 3.5 | Interview progresses through a state machine (SETUP → QUESTION → ANSWER → EVALUATION → ... → COMPLETED) | P0 |
| 3.6 | System produces a final report (overall score, strengths, weaknesses, recommendations) | P0 |
| 3.7 | Structured questions/evaluation, adaptive difficulty, follow-up questions, JD-based interview config | P1 |
| 3.8 | Web search for version-specific/fresh knowledge (e.g. current framework versions) | P1 |
| 3.9 | Fully adaptive interviewer, resume-based interview, performance tracking across sessions | P2/P3 |

## 4. Cross-Feature Integration (not MVP)

| # | Use Case | Priority |
|---|----------|----------|
| 4.1 | Discuss CV analysis results inside chat | P1 |
| 4.2 | Discuss interview results inside chat | P1 |
| 4.3 | Generate interview configuration from CV or JD | P2 |

## Recommended Build Order

1. Shared AI infrastructure (0.1–0.4)
2. Basic Chat (1.1–1.6) — validates the AI service end-to-end with the simplest workflow
3. CV Review + CV/JD Analysis (2.1–2.4) — validates document handling and structured output
4. Mock Interview (3.1–3.6) — validates stateful, multi-turn AI workflows

This order matches [docs/06-roadmap-scope.md](./06-roadmap-scope.md) section 29: prove three
workflows of a genuinely different nature on top of one well-designed AI infrastructure, rather
than building every capability for one workflow first.
