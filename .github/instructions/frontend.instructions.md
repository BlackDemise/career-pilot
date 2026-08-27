---
description: "Use when writing or modifying frontend code in fe/: React components, hooks, API calls, routing, or state management."
applyTo: "fe/**"
---

# Frontend Instructions (React + TypeScript + Vite)

- Use functional components with hooks only; no class components.
- Organize code by feature under `src/features/<feature>/` (`chat`, `cv-analysis`, `interview`),
  with cross-feature code under `src/shared/`. See
  [docs/03-frontend-architecture.md](../../docs/03-frontend-architecture.md).
- All backend calls go through a single shared HTTP client module under `src/shared/api/`. Do
  not call `fetch` directly from components.
- Keep TypeScript strict; avoid `any`. Put shared types under `src/shared/types/` and colocate
  feature-specific types with that feature.
- Never embed API keys or secrets in frontend code. The Gemini key stays server-side only; the
  frontend talks exclusively to the backend under `/api/**` (see
  [api.instructions.md](./api.instructions.md) and `nginx/nginx.conf`).
- Follow the existing ESLint config (`fe/eslint.config.js`); do not disable rules inline without
  a stated reason.
- Use environment variables via Vite's `import.meta.env`, prefixed `VITE_`, for any
  frontend-configurable value. Never read server-side env vars from the frontend.
- Prefer composition over prop drilling. Use React Context only for genuinely cross-cutting
  concerns (e.g. auth/session), not as a general state container.
