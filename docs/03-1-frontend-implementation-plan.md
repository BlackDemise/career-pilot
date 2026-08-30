# Frontend Implementation Plan

This document turns [02-use-cases.md](./02-use-cases.md) and the backend contracts into frontend
work. Priorities describe when the frontend should be built: P0 is available now or required for
the MVP, while P1, P2, and P3 depend on backend work that is not currently available. "Backend code
exists" is not treated as complete when the revised live-interview contract still needs end-to-end
verification.

## 0. Baseline Dependencies

Install this baseline before implementing feature screens. It deliberately pays the setup cost for
the routing, server-state, WebSocket, markdown, and testing patterns that the final application will
need.

| Package | Type | Use in CareerPilot | Completed |
|---|---|---|---|
| `react-router-dom` | dependency | Public auth routes, protected application routes, nested shell routes, and redirects | Yes |
| `axios` | dependency | Shared HTTP client for `/api/v1/**`, JSON and multipart requests, credentials, and auth interceptors | Yes |
| `@tanstack/react-query` | dependency | Server queries/mutations for profile, conversations, CVs, analyses, catalogs, sessions, and reports | Yes |
| `react-use-websocket` | dependency | Active interview connection lifecycle and reconnect handling behind a typed feature hook | Yes |
| `react-markdown` | dependency | Assistant response rendering when chat markdown arrives in P1 | Yes |
| `rehype-sanitize` | dependency | Sanitization of model-controlled markdown before it becomes DOM | Yes |
| `lucide-react` | dependency | Accessible icon controls and status indicators | Yes |
| `clsx` | dependency | Conditional CSS Module class composition; it is not the styling system itself | Yes |
| `vitest` | devDependency | Unit and component test runner integrated with Vite | Yes |
| `jsdom` | devDependency | Browser-like DOM environment for component tests | Yes |
| `@testing-library/react` | devDependency | Behavior-focused React rendering and queries | Yes |
| `@testing-library/user-event` | devDependency | Realistic keyboard, pointer, and form interaction tests | Yes |
| `@testing-library/jest-dom` | devDependency | Readable DOM matchers for visibility, roles, values, and document state | Yes |

Styling remains CSS Modules plus global CSS variables/reset. `clsx` is only the composition helper.
Configure Vitest with the `jsdom` environment and a setup file importing `@testing-library/jest-dom`.
Do not add a second HTTP client, global state library, UI component framework, or WebSocket library.

## 1. Foundation and Authentication

| # | Frontend use case | Priority | Backend status | Frontend deliverable | Completed |
|---|---|---|---|---|---|
| F0.1 | App entry point, route tree, error/loading/empty states | P0 | Available | Router, providers, protected layout, not-found and forbidden pages | No |
| F0.2 | Shared `ApiResponse<T>` client and typed errors | P0 | Available | One fetch client with wrapper parsing and validation-map errors | Yes |
| F0.3 | Login, access-token session, logout, refresh-once retry | P0 | Available | Auth context/store, storage policy, session bootstrap, redirect behavior | Yes |
| F0.4 | Registration and verification link flow | P0 | Available | Register form, token query parsing, pending-registration page, resend cooldown | Yes |
| F0.5 | Forgot password and reset link flow | P0 | Available | Generic request result, pending-reset page, resend cooldown, reset form | Yes |
| F0.6 | Authenticated shell and navigation | P0 | Available | Chat, CV, Interview, Profile navigation with mobile layout | Yes |

The Axios client must set `withCredentials: true` so the browser sends the backend-managed cookie.
Its response interceptor needs a refresh lock or shared refresh promise so concurrent 401 responses
do not rotate the same refresh token in parallel. The refresh request is sent to
`POST /api/v1/auth/refresh`, then the original request is retried once. Refresh calls themselves
must bypass the retry loop. Logout is best effort locally and remotely; clear the access token even
if the request fails. The UI must not infer account existence from forgot-password responses.

## 2. Chat and Profile

| Use cases | Priority | Backend status | Frontend work | Completed |
|---|---|---|---|---|
| 1.1 Create conversation | P0 | Implemented | Conversation list, new conversation action, title state | No |
| 1.2 Send and receive message | P0 | Implemented | Message timeline, composer, submit lock, request error recovery | No |
| 1.3 Save and reload history | P0 | Implemented | List/detail loading, selected conversation route, empty states | No |
| 1.4 Global profile instructions | P0 | Implemented | Profile form for language, style, background, goal, custom instructions | No |
| 1.5 Career/technology scope | P0 | Implemented by backend prompt | Render refusal as a normal assistant message; do not duplicate policy in UI | No |
| 1.6 Delete conversation | P0 | Implemented | Confirmed destructive action and list reconciliation | No |
| 1.7 Streaming, retry, edit, markdown | P1 | Not implemented | Add only after matching backend transport and response semantics exist | No |
| 1.8 Intent classification | P1 | Not implemented | Add visible context indicators only if API exposes a client-safe contract | No |
| 1.9 Conversation summaries | P1 | Not implemented | Add summary/loading states once endpoint and persistence exist | No |
| 1.10 Search, semantic memory, web grounding | P2/P3 | Not implemented | Deferred; no client architecture should assume RAG or retrieval | No |

The first chat implementation should use a route-level conversation selection and local pending
message state. React Query owns conversation/profile server state; local component state owns the
composer, selected tab, and transient submit state. Profile updates invalidate or refresh the
profile query and are clearly separated from conversation messages. The API module targets:
`POST/GET /api/v1/conversations`, `GET/DELETE /api/v1/conversations/{conversationId}`, and
`POST /api/v1/conversations/{conversationId}/messages`.

## 3. CV Analysis

| Use cases | Priority | Backend status | Frontend work | Completed |
|---|---|---|---|---|
| 2.1 Upload PDF CV | P0 | Implemented | File picker, PDF/type/size feedback, upload progress state | No |
| 2.2 Extract text | P0 | Implemented | Upload result view; do not expose raw text as the primary result layout | No |
| 2.3 CV review | P0 | Implemented | Review action, structured assessment, strengths, weaknesses, recommendations | No |
| 2.4 CV + JD match | P0 | Implemented | JD editor, match score, matched/missing skills, gaps, recommendations | No |
| 2.5 DOCX/TXT and structured extraction | P1 | Not implemented | Extend accepted-file UI only with backend support | No |
| 2.6 Section scoring and requirement categories | P1 | Not implemented | Add section comparison views after typed DTOs are defined | No |
| 2.7 Evidence mapping and ATS analysis | P2 | Not implemented | Deferred evidence view and confidence visualization | No |
| 2.8 CV rewrite and tailoring | P3 | Not implemented | Deferred; requires a separate editing workflow and explicit backend contract | No |

The feature should keep CV records and analyses distinct: upload once, select a CV, then request
review or JD match. Use Axios multipart `FormData` for `POST /api/v1/cvs` and JSON requests for
`POST /api/v1/cvs/{cvId}/analyses/review` and `/analyses/jd-match`; load history from
`GET /api/v1/cvs/{cvId}/analyses`. Analysis responses are typed discriminated unions by analysis
type; rendering must handle partial or empty arrays without assuming Gemini returned prose.

## 4. Mock Interview

| Use cases | Priority | Backend status | Frontend work | Completed |
|---|---|---|---|---|
| 3.1 Catalog-backed setup | P0 | Code exists, needs live verification | Load catalogs, dependent role/level/topic selection, duration and min/max budgets | No |
| 3.2 Compatibility and valid plan | P0 | Code exists, needs live verification | Display backend validation errors; never compute compatibility as authority in React | No |
| 3.3 Live phased WebSocket interview | P0 | Code exists, needs live verification | Native WebSocket hook, typed event reducer, reconnect/closed states | No |
| 3.4 Answer deadlines and timeouts | P0 | Code exists, needs live verification | Server-synchronized countdown, timeout submission, disabled stale controls | No |
| 3.5 State, phase, counters, early ending | P0 | Code exists, needs live verification | Phase indicator and counters driven by server events | No |
| 3.6 Integrity events and full screen | P0 | Code exists, needs live verification | Full-screen request, visibility/focus listeners, event throttling, neutral status display | No |
| 3.7 Final evaluation after completion | P0 | Code exists, needs live verification | Completion transition, report fetch, final score and recommendations | No |
| 3.8 Structured/adaptive/JD setup | P1 | Not available as final contract | Extend setup and event types only after backend DTOs settle | No |
| 3.9 Fresh web knowledge | P1 | Not implemented | Deferred source/loading/error UI | No |
| 3.10 Fully adaptive/resume/performance history | P2/P3 | Not implemented | Deferred | No |

The interview reducer must reject events that do not match the current session identifier or cannot
advance the known state. `react-use-websocket` should be isolated behind `useInterviewSocket`, which
maps JSON messages to a typed event union and sends the backend's `ANSWER_SUBMITTED`,
`ANSWER_TIMEOUT`, and `INTEGRITY_EVENT` payloads. HTTP targets `GET /api/v1/interviews/catalog`,
`POST /api/v1/interviews`, `GET /api/v1/interviews/{sessionId}`, and
`GET /api/v1/interviews/{sessionId}/report`.

The timer is a display derived from `endsAt` and server question deadlines; it never extends a
session. Browser integrity signals are observations, not accusations or automatic score changes.
The current query-string WebSocket access-token contract is a backend-documented interim design and
must be isolated in one connector so it can later move to a short-lived ticket. Handle
`SESSION_STARTED`, `INTERVIEWER_MESSAGE`, `ANSWER_TIMEOUT`, `PHASE_CHANGED`, `INTERVIEW_ENDING`,
`INTERVIEW_COMPLETED`, and `ERROR` without exposing intermediate evaluations.

## 5. Shared Types and API Modules

Create these contracts before feature screens:

```text
shared/api/
|- axiosClient.ts      # base URL, credentials, bearer header, 401/403 policy
|- apiResponse.ts      # ApiResponse<T>, unwrap, ApiValidationError
`- authSession.ts      # access-token storage and refresh coordination

shared/types/
|- api.ts              # wrapper and common error types
|- auth.ts             # claims-safe session/user types
|- profile.ts
`- dates.ts            # API timestamp/ISO conversion policy
```

Feature modules should define request/response types beside their API functions and use React Query
keys beside the query functions they invalidate. Use string literal unions for backend enums such as
`USER`, `ASSISTANT`, interview phases, statuses, and event types. Do not type an API result as
`any`; use `unknown` at the parser boundary and validate or narrow it before rendering. Keep UUIDs
as strings and convert dates in view helpers rather than mutating the transport model.

## 6. Testing Gates

The baseline includes Vitest, React Testing Library, `user-event`, `jest-dom`, jsdom, and the
required Vite configuration. Follow [05-testing-strategies.md](./05-testing-strategies.md): mock the
Axios client or feature API boundary, never call a live backend, and test observable behavior.

Minimum gates by slice:

| Slice | Required tests |
|---|---|
| API foundation | Wrapper success/error parsing, validation map, 401 refresh once, repeated 401 logout, 403 handling |
| Auth | Form validation, pending-page cooldown, token query verification/reset, redirect after verification |
| Chat | Load/select/create/send/delete behavior and profile update feedback |
| CV | File validation, upload state, review/JD result rendering, empty analysis arrays |
| Interview | Event reducer transitions, stale event rejection, deadline display, timeout/integrity actions, completion report |
| App shell | Protected route behavior, loading state, forbidden and not-found routes |

Manual verification is still needed for same-origin cookies, nginx proxying, WebSocket lifecycle,
browser full-screen/visibility events, and mobile layout. These are integration concerns and cannot
be proven by component tests alone.

## 7. Delivery Order

1. Add frontend test tooling, routing, CSS tokens, shared API types, and HTTP client.
2. Implement auth pages and session behavior, then verify refresh and cookie behavior through nginx.
3. Implement the authenticated shell and profile settings.
4. Implement chat P0, including conversation history and deletion.
5. Implement CV P0, including upload, review, JD match, and analysis history.
6. Verify the live interview backend contract, then implement setup, WebSocket reducer, timer,
   integrity events, reconnect states, and final report.
7. Add focused tests and manual workflow checks after each slice.
8. Take P1 work only when the corresponding backend endpoint or protocol is available and documented.

This order follows the backend dependency order while giving the frontend one shared session and
error model before feature code begins.
