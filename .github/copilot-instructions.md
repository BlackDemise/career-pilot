# CareerPilot Agent Instructions

## Project

CareerPilot is an AI-assisted career platform (CV review, CV/JD matching, mock interviews,
career-focused chat) built with a Spring Boot backend (`be/`) and a React + TypeScript
frontend (`fe/`), fronted by nginx, backed by PostgreSQL (pgvector image) and Google Gemini.

## Key Docs

- Introduction & setup: [docs/01-introduction.md](../docs/01-introduction.md)
- Use cases & priority order: [docs/02-use-cases.md](../docs/02-use-cases.md)
- Frontend architecture: [docs/03-frontend-architecture.md](../docs/03-frontend-architecture.md)
- Backend architecture: [docs/04-backend-architecture.md](../docs/04-backend-architecture.md)
- Testing strategy: [docs/05-testing-strategies.md](../docs/05-testing-strategies.md)
- Full feature scope & roadmap (MVP/V1/V2/Advanced): [docs/06-roadmap-scope.md](../docs/06-roadmap-scope.md)

## Mandatory Process

Every prompt that results in a codebase change (code, config, docs, or instructions) MUST be
logged in [docs/00-prompts.md](../docs/00-prompts.md) with: the user's prompt, your assessment,
what you did, what you could not do, and alternatives. Do not skip or shorten this, and do not
summarize past entries away — each is evidence of what was done and why.

## Build & Run

- Backend: `be/mvnw spring-boot:run` (Java 21, Maven, Spring Boot 4)
- Frontend: `npm run dev` in `fe/` (Vite, React 19)
- Full stack: `docker compose up` from repo root (requires `.env`, see `.env.example`)

## Scope Discipline

This project intentionally limits scope to avoid over-engineering (see the "Red Advanced" list
in [docs/06-roadmap-scope.md](../docs/06-roadmap-scope.md)). Do not introduce RAG, vector search,
multi-agent orchestration, voice interviews, or microservices unless explicitly requested.
