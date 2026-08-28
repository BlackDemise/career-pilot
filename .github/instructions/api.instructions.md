---
description: "Use when designing or modifying REST API endpoints, request/response DTOs, or the contract between fe/ and be/."
applyTo: ["be/**/controller/**", "be/**/dto/**", "fe/**/api/**"]
---

# API Contract Instructions

- All backend endpoints are exposed under `/api/v1/**` (see `nginx/nginx.conf` routing, which
  proxies the whole `/api/` prefix). Do not add routes outside this prefix; bump to `/api/v2/**`
  only for breaking changes, keeping the old version alive until clients migrate.
- Use plural, resource-based paths: `/api/v1/conversations`, `/api/v1/cvs`, `/api/v1/interviews`
  — not verb-based paths like `/api/v1/getConversation`. The auth endpoints
  (`/api/v1/auth/register|register/verify|register/resend|login|refresh|logout|forgot-password|
  forgot-password/resend|reset-password`) are the deliberate exceptions, since they represent
  authentication actions rather than resources.
- `/api/v1/auth/**` is the only publicly reachable (unauthenticated) group; every other
  `/api/v1/**` route requires a valid, non-blacklisted JWT access token (see
  `JwtAuthenticationFilter`).
- Request/response bodies are JSON with camelCase field names on both frontend and backend.
- Structured AI outputs (CV review, CV/JD match, interview evaluation, etc.) must be returned as
  typed DTOs matching the structures described in
  [docs/06-roadmap-scope.md](../../docs/06-roadmap-scope.md), never as raw model text.
- Every endpoint returns the same wrapper:
  `{ "timestamp": 0, "statusCode": 200, "message": "...", "result": null }`.
  Normal responses put their payload in `result`; business errors use `result: null`; validation
  errors use a general message and put a `Map<String, String>` of field errors in `result`.
  The timestamp is epoch seconds and `statusCode` is the numeric HTTP status.
- Use standard HTTP status codes: 200/201 success, 400 validation error, 401/403 auth, 404 not
  found, 500 unexpected error.
- When a DTO shape changes, update both the `be/` DTO and the corresponding `fe/` API type in
  the same change — never let them drift.
- Do not leak internal-only entity fields (surrogate keys not needed by the client, audit
  columns, etc.) through the API unless the frontend has a real need for them.
