# Mock Interview Backend Design

This document defines the revised Mock Interview scope. The feature is a live, turn-by-turn AI
interviewer conducted over a WebSocket. It is not a pre-generated questionnaire that immediately
reveals an evaluation after each answer.

## Product Contract

The candidate selects a supported role and level, then selects one or more topics offered by the
backend for that role and level. The candidate configures an interview duration, up to a maximum of
60 minutes, and a question budget with both minimum and maximum primary-question thresholds.
Difficulty is not user-configurable. The backend and interviewer derive question complexity from
the role, level, topic, phase, and observed conversation.

The candidate sees natural interviewer messages during the session. Intermediate scores,
strengths, weaknesses, and corrective feedback are internal data only. The final report is shown
only after the interview has ended and includes the overall score, strengths, weaknesses, and
recommendations.

## Configuration Catalogs

Catalog data is database-backed from the first implementation. This costs more than constants at
the beginning, but it supports future career domains, controlled content changes, weighting, and
administration without changing application code.

The initial catalog should contain stable identifiers and display labels for supported roles, for
example `WEB_DEVELOPER`, `ANDROID_DEVELOPER`, `IOS_DEVELOPER`, `BACKEND_DEVELOPER`,
`FRONTEND_DEVELOPER`, `FULL_STACK_DEVELOPER`, `DEVOPS_ENGINEER`, `QA_ENGINEER`,
`DATA_ENGINEER`, `DATA_SCIENTIST`, `MACHINE_LEARNING_ENGINEER`, and `SECURITY_ENGINEER`.
There is deliberately no `OTHER` role. Unsupported roles must be added to the catalog explicitly.

Levels initially include `INTERN`, `FRESHER`, `JUNIOR`, `MIDDLE`, and `SENIOR`.

Topics are offered through role-level compatibility records rather than a global list. A topic
record should be able to express:

- Whether it is available for a role and level.
- Whether it is required in a valid plan.
- Whether it can be selected by a candidate.
- Its selection weight for random plans.
- Its expected minimum and maximum primary-question coverage.
- Its phase, such as `WARM_UP`, `TECHNICAL`, or `SITUATIONAL`.

This makes the rule explicit: `role + level -> valid topics`. Interns and freshers simply do not
receive advanced topics unless those topics are intentionally inserted into their catalog rules.
The API must validate submitted IDs against the database even when the frontend loaded the same
catalog earlier.

## Random Plans

Randomization is constrained selection, not unrestricted randomness and not a Gemini decision.
The backend creates a plan from compatible catalog records using these rules:

1. Include every required topic or phase requirement.
2. Select optional topics from the valid role-level pool using configured weights.
3. Do not repeat a topic unless its catalog record explicitly permits repetition.
4. Avoid selecting more topics than the question budget and duration can support.
5. Persist the selected plan and its topic order before the interview begins.

The database determines whether repetition is allowed. Static code does not need to guess: a
catalog field such as `allowRepeat` or a plan-rule record makes the policy data-driven. A unique
constraint over `(session_id, topic_id)` can additionally protect non-repeatable selections at the
persistence boundary.

The number of questions should not be chosen only by a random number in the configured range. The
backend should validate the requested minimum and maximum against duration and catalog constraints,
then choose a valid target within that range for a random plan. The maximum is a hard budget; the
minimum is a completion floor unless duration expires or a controlled early-ending policy applies.
Follow-up turns should consume an interaction budget so an interview cannot run indefinitely even
when the primary-question maximum has not been reached.

## Duration and Budget

The session stores at least:

- `durationSeconds` and an absolute `endsAt` deadline.
- `minimumPrimaryQuestions` and `maximumPrimaryQuestions`.
- `primaryQuestionsAsked` and `totalTurns`.
- `currentPhase` and the current question/turn identifier.
- The persisted selected plan.

The backend is the source of truth for all counters and deadlines. The frontend may display a
countdown, but it cannot extend the interview by changing its local clock. A 60-minute duration
and 30 primary questions is a reasonable hard maximum, but the default should be shorter to keep
the first experience manageable. A future configuration catalog can provide recommended defaults.

The budget has three independent constraints:

- The interview cannot finish normally before the minimum primary-question threshold.
- It must end by the duration deadline.
- It must end at or before the maximum primary-question or total-turn budget.

An early ending recommendation is accepted only when backend policy permits it. For example, the
backend may require the introduction, a minimum warm-up, and enough main-section evidence before
allowing an early close.

## Interview Phases and State Machine

The durable session state is:

```text
SETUP -> PREPARING -> INTRODUCTION -> WARM_UP -> TECHNICAL -> SITUATIONAL -> CLOSING -> COMPLETED
```

`FAILED` and `EXPIRED` may be needed for infrastructure and reconnect handling, but they are not
normal candidate outcomes. Turn-level state is transient:

```text
ASKING -> WAITING_FOR_ANSWER -> PROCESSING_ANSWER -> DECIDING_NEXT_ACTION -> ASKING
```

The phases have distinct purposes:

- `INTRODUCTION`: interviewer identity, expectations, timing, and conduct.
- `WARM_UP`: candidate background and recent experience.
- `TECHNICAL`: role-level topic questions and bounded follow-ups.
- `SITUATIONAL`: working-situation and behavioral questions.
- `CLOSING`: graceful conclusion and final-report preparation.

The AI can select the next question or recommend a phase transition, but the backend checks the
current state, counters, required topics, and remaining time before applying it.

## WebSocket Responsibilities

HTTP endpoints remain appropriate for:

```text
GET  /api/v1/interviews/catalog
POST /api/v1/interviews
GET  /api/v1/interviews/{sessionId}
GET  /api/v1/interviews/{sessionId}/report
```

The live session uses:

```text
/api/v1/interviews/{sessionId}/stream
```

The WebSocket protocol should support typed events such as `SESSION_STARTED`, `INTERVIEWER_MESSAGE`,
`ANSWER_SUBMITTED`, `ANSWER_TIMEOUT`, `TIMER_WARNING`, `PHASE_CHANGED`, `INTERVIEW_ENDING`,
`INTERVIEW_COMPLETED`, and `ERROR`. Messages should contain event IDs or sequence numbers so a
reconnecting client can identify missed events.

The connection is authenticated with the existing JWT model, and the backend verifies session
ownership before accepting it. The first MVP may send complete interviewer messages rather than
Gemini token streams; token-level streaming remains separate shared-AI scope.

## Timeout and Window Integrity

The server starts an answer deadline when it sends a question. The frontend can show the remaining
time, but only the server decides that the deadline expired. A timeout records an unanswered turn,
emits `ANSWER_TIMEOUT`, and asks Gemini to continue naturally without exposing an intermediate
score. The interviewer should say something like, “Let’s move on to another area,” rather than
announcing a failure.

During an active interview the frontend requests full-screen mode and reports browser integrity
events, including `FOCUS_LOST`, `TAB_HIDDEN`, `FULLSCREEN_EXITED`, and `WINDOW_BLURRED`. The server
stores the event type, timestamp, session, turn, and client-provided metadata, then broadcasts a
corresponding status event when appropriate.

These signals are evidence of a window violation, not proof that a candidate cheated. Browsers can
deny or interrupt full-screen mode, and client events cannot be made tamper-proof by WebSocket
alone. The product should therefore record counts and surface them as integrity observations in
the final report or reviewer view, while avoiding an automatic accusation or automatic score
penalty in the initial version. Repeated events may trigger a warning or a backend-controlled
termination policy later.

## AI Boundary and Ending Policy

Gemini receives the validated interview plan, current phase, remaining time, question counters,
previous interviewer turns, and candidate answers as context. It may recommend:

```text
CONTINUE
ASK_FOLLOW_UP
MOVE_TO_NEXT_TOPIC
MOVE_TO_NEXT_PHASE
RECOMMEND_EARLY_END
```

The backend does not give Gemini unrestricted authority. It applies deterministic rules first:

- Duration timeout always moves the session toward `CLOSING`.
- Maximum budgets always stop further questions.
- Early ending is disallowed before the minimum question floor and required phases/evidence.
- One poor or excellent answer alone is not enough to terminate the interview.
- Candidate requests to stop are explicit, auditable termination events.

The final report prompt receives the complete persisted interview record, including unanswered or
timed-out questions and integrity observations where appropriate. No final evaluation is sent over
the live channel before `COMPLETED`.

## Persistence Model

The existing question/answer/evaluation tables are insufficient for this revised behavior without
additional fields and records. The target model should include:

- Catalog tables for roles, levels, topics, role-level-topic compatibility, and plan rules.
- `InterviewSession` configuration, deadline, counters, current phase, state, and selected plan.
- `InterviewTurn` or expanded question records with phase, sequence, status, and timestamps.
- `InterviewAnswer` with submission time and timeout marker.
- Internal observations separate from candidate-facing final evaluation.
- `InterviewIntegrityEvent` for focus, tab, full-screen, and related browser events.
- Final evaluation/report persistence if reports must be reproducible without another Gemini call.

Schema changes must be introduced through Flyway migrations. Entities must remain behind the
service layer, and all WebSocket and HTTP responses must use typed DTOs where they cross the API
boundary.

## Revised Delivery Order

The old batch implementation should be treated as a prototype and replaced in this order:

1. Database catalogs and deterministic configuration validation.
2. Session model, phase state machine, duration, and question budgets.
3. WebSocket authentication, connection lifecycle, and typed events.
4. Turn-by-turn interviewer orchestration with deferred evaluation.
5. Server-side answer timeout and graceful closing.
6. Browser integrity event recording and full-screen request flow.
7. Final report generation and persistence.
8. Unit, WebSocket slice, and integration tests for state transitions, ownership, deadlines,
   catalog validation, and event ordering.

Adaptive difficulty, sophisticated follow-up strategy, JD/resume-based plans, web search, voice,
speech analysis, and cross-session performance remain later priorities.