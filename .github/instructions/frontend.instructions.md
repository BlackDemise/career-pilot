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

## Before Coding

Inspect existing components, hooks, utils, and styles before adding new ones; reuse before
creating. Identify the smallest set of files that need to change. Do not rewrite or restructure
unrelated code, rename things unnecessarily, or remove existing functionality without a reason.

## React Conventions

- Components have a single clear responsibility; prefer small, composable components over large
  ones. Do not wrap a few lines of JSX in a component unless it improves reuse or readability.
- Follow the Rules of Hooks: never call hooks conditionally, in loops, or in nested functions.
  Extract reusable stateful behavior into custom hooks.
- Prefer deriving values during render over synchronizing derived state with `useEffect`. Keep
  side effects (subscriptions, imperative DOM/browser API calls) out of the render body.
- Keep state as local as possible; do not duplicate derived state; lift state only when multiple
  components genuinely need it. Never mutate state or props directly.
- Every data-driven UI must explicitly handle loading, success, empty, error, and disabled states,
  not just the ideal successful path.
- Never swallow errors silently (no empty `catch` blocks); surface them to the UI or propagate
  them to an error boundary.
- Do not add `useMemo`/`useCallback`/`React.memo` speculatively; add them when they solve an
  observed performance problem.
- Architecture flow: UI components -> feature components -> hooks/application logic -> API/
  services. Keep network calls and business logic out of presentational components.
- Use semantic HTML (`button`, `a`, `nav`, `header`, `main`, `section`, `form`, `label`) instead of
  `div`/`span` with click handlers. Provide visible focus states and accessible labels. Do not use
  `dangerouslySetInnerHTML` without sanitization (see `rehype-sanitize` for markdown content).
- Before considering frontend work complete, run `npm run lint` and `npm run build` in `fe/` and
  fix any errors introduced by the change.
- Run `npm run test` to verify test files are written correctly. If Vitest fails with
  `TypeError: Cannot read properties of undefined (reading 'config')` at the describe-block phase
  (before any tests execute), this is a known project-level Vitest/jsdom/environment initialization
  issue that does not reflect test code quality. In this case, confirm lint and build pass, verify
  the test files are syntactically correct and importable, then skip the test execution. This issue
  can be addressed in a separate Vitest infrastructure session.
