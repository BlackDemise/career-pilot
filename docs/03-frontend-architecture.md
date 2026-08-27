# Frontend Architecture

This document describes the intended folder structure of `fe/` and the reasoning behind it. The
codebase currently only has the Vite/React scaffold (`src/main.tsx`, `src/App.tsx`); this is the
target structure to grow into as features are implemented.

## Target Folder Structure

```
fe/src/
├── main.tsx                 # entrypoint, mounts App
├── App.tsx                  # root component, routing/providers
├── app/                     # app shell: router, layout, global providers
│   ├── router.tsx
│   └── layout/
├── features/                 # one folder per business feature (matches docs/02-use-cases.md)
│   ├── chat/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api.ts            # feature-specific API calls, built on shared client
│   │   └── types.ts
│   ├── cv-analysis/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api.ts
│   │   └── types.ts
│   └── interview/
│       ├── components/
│       ├── hooks/
│       ├── api.ts
│       └── types.ts
├── shared/                   # cross-feature code
│   ├── api/                  # shared HTTP client, error handling, base fetch wrapper
│   ├── components/           # generic UI components (Button, Modal, etc.)
│   ├── hooks/                 # generic hooks (useDebounce, etc.)
│   ├── types/                 # shared types (error shape, pagination, user profile)
│   └── utils/
└── config/                   # env var access, constants
```

## Reasoning

- **Feature-based over layer-based**: Chat, CV Analysis, and Mock Interview are the three
  independent workflows described in [docs/02-use-cases.md](./02-use-cases.md). Grouping by
  feature keeps each workflow's components, hooks, and API calls together, and lets each grow
  independently without one giant `components/` or `hooks/` folder.
- **`shared/` is intentionally small**: only code genuinely reused across features belongs here.
  Anything feature-specific stays inside that feature's folder to avoid premature abstraction.
- **Single API client**: all HTTP calls are funneled through `shared/api/`, which wraps `fetch`
  and centralizes base URL (`/api`), error parsing (matching the error shape in
  [api.instructions.md](../.github/instructions/api.instructions.md)), and auth headers once
  authentication exists. Feature `api.ts` files only define feature-specific endpoint calls.
- **No premature routing/state library**: the MVP scope (three screens: Chat, CV, Interview)
  does not need a heavy router or global state library. Start with whatever minimal routing
  approach fits (even simple conditional rendering), and only introduce a router/state library
  once the number of screens/state complexity justifies it.

## Open Decisions (not yet fixed)

These are flagged rather than assumed, since the current `package.json` has no opinion on them
yet:

- **Styling approach**: no CSS framework/library is installed yet (no Tailwind, no CSS-in-JS).
  Recommend starting with plain CSS Modules per component to keep the dependency footprint
  minimal, revisiting only if styling needs grow.
- **Server-state/data-fetching library**: plain `fetch` + hooks is enough for MVP. A library
  like TanStack Query is worth considering once caching/refetching/streaming needs grow (V1+),
  not before.
