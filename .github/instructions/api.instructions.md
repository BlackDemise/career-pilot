---
description: "Use when designing or modifying REST API endpoints, request/response DTOs, or the contract between fe/ and be/."
applyTo: ["be/**/controller/**", "be/**/dto/**", "fe/**/api/**"]
---

# API Contract Instructions

- All backend endpoints are exposed under `/api/**` (see `nginx/nginx.conf` routing). Do not
  add routes outside this prefix.
- Use plural, resource-based paths: `/api/conversations`, `/api/cvs`, `/api/interviews` — not
  verb-based paths like `/api/getConversation`.
- Request/response bodies are JSON with camelCase field names on both frontend and backend.
- Structured AI outputs (CV review, CV/JD match, interview evaluation, etc.) must be returned as
  typed DTOs matching the structures described in
  [docs/06-roadmap-scope.md](../../docs/06-roadmap-scope.md), never as raw model text.
- Error responses use one consistent shape across the API:
  `{ "error": { "code": "...", "message": "..." } }`, produced by a global exception handler.
- Use standard HTTP status codes: 200/201 success, 400 validation error, 401/403 auth, 404 not
  found, 500 unexpected error.
- When a DTO shape changes, update both the `be/` DTO and the corresponding `fe/` API type in
  the same change — never let them drift.
- Do not leak internal-only entity fields (surrogate keys not needed by the client, audit
  columns, etc.) through the API unless the frontend has a real need for them.
