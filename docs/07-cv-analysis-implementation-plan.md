# CV Analysis Implementation Plan

This document is the implementation reference for CV use cases 2.5 through 2.8 in
[02-use-cases.md](./02-use-cases.md). It records the decisions agreed before implementation and
the order in which the backend and frontend contracts will be introduced.

## Scope

The work covers:

- PDF and DOCX upload with content validation
- lazy, versioned structured CV extraction
- two-step job-description requirement extraction and CV matching
- deterministic section and overall match scoring
- evidence grounding and verification
- separate ATS-oriented analysis
- immutable rewrite suggestions and, later, derived tailored CV versions
- asynchronous processing with persisted jobs and SSE notifications

TXT is intentionally excluded from the first implementation. Legacy binary `.doc` files are also
excluded until explicit support is added; a `.doc` file must not be treated as a DOCX file.

## Decisions

### Upload and extraction

Upload is synchronous and must not call Gemini. The original document and extracted raw text are
stored first. Structured extraction runs lazily when the first analysis needs it. Upload and
analysis are separate frontend actions.

The original CV is immutable. Any generated or tailored document is a new derived CV/version and
never overwrites its source.

### File validation

The backend validates extension, declared content type, and file signature/container:

- PDF must have a valid PDF signature and be readable by PDFBox.
- DOCX must be a valid ZIP/Open XML package containing the expected Word document parts.
- Request headers and filenames are hints, not proof of file type.
- Maximum size is enforced before parsing.

### Structured extraction

The canonical extraction contains education, experience, projects, and skills. It may also contain
summary, certifications, and languages. Extracted entries retain source block identifiers and
source text so later evidence can be checked against the original CV text.

Structured results are stored as versioned JSON with extraction status and timestamps. Raw text is
retained unchanged.

### Requirement categories

The AI infers `REQUIRED`, `PREFERRED`, or `OPTIONAL` from the JD. Explicit wording wins:

- `must`, `required`, `mandatory`, `essential`, and `minimum` imply `REQUIRED`.
- `preferred`, `desired`, `beneficial`, and `advantageous` imply `PREFERRED`.
- `nice to have`, `bonus`, `optional`, and `extra credit` imply `OPTIONAL`.

Section headings provide context. Unmarked items in general requirements or qualifications sections
default to `REQUIRED` when tied to a minimum qualification or essential responsibility; otherwise
they default to `PREFERRED`. Optional is reserved for clearly secondary or bonus items.

Each requirement retains category confidence, rationale, and source text.

### Match score

The AI returns requirement-level judgments and evidence. The backend calculates the canonical score.

Initial category weights are:

| Category | Weight |
|---|---:|
| REQUIRED | 1.0 |
| PREFERRED | 0.6 |
| OPTIONAL | 0.25 |

Initial coverage values are:

| Match status | Coverage |
|---|---:|
| SUPPORTED | 1.0 |
| PARTIALLY_SUPPORTED | 0.5 |
| UNCLEAR | 0.25 |
| NOT_SUPPORTED | 0.0 |

The overall score is the weighted average of requirement coverage, scaled to 0-100. Section
scores use the same requirement judgments for requirements mapped to each section; they are not
independent model-generated scores.

### Evidence verification

The model returns source block IDs, an evidence quote, and model confidence. The backend verifies
that referenced blocks belong to the current CV and that the quote occurs in the source text after
normalizing whitespace.

The backend records verification as `VERIFIED_EXACT`, `VERIFIED_NORMALIZED`, `UNVERIFIED`, or
`INVALID_REFERENCE`. Unverified or invalid evidence cannot contribute positive match coverage and
is represented as `UNCLEAR` at most. Model confidence is informative; it is not trusted as proof.

### ATS analysis

ATS analysis is a separate analysis type and endpoint. It combines deterministic checks, such as
extractability, standard headings, keyword presence, and date consistency, with AI-generated
semantic recommendations. It is not part of the JD match score.

### Asynchronous processing

Analysis requests return `202 Accepted` with a persisted job ID. Jobs expose current lifecycle
stages rather than pretending Gemini can report a meaningful percentage:

`QUEUED`, `EXTRACTING_CV`, `EXTRACTING_JD_REQUIREMENTS`, `MATCHING_REQUIREMENTS`,
`VERIFYING_EVIDENCE`, `RUNNING_ATS_ANALYSIS`, `GENERATING_REWRITE`, `COMPLETED`, `FAILED`.

The status endpoint is the source of truth. SSE publishes lifecycle notifications while the client
is connected; the frontend can reconnect or use status polling after navigation. WebSockets are not
needed for this workflow. The first implementation uses a persisted database job and an
application-managed executor; a message broker is deferred until scale requires it.

## Data Model Direction

The existing raw CV text remains on `cvs`. The first migration set adds versioned extraction data
and a persisted analysis-job record. Generated analysis results remain typed JSON initially rather
than being split into many tables.

Data should be normalized only when it needs independent querying, editing, or lifecycle
management. Evidence starts inside the typed analysis result because it belongs to one analysis.
A future derived-CV record is required before generated documents can be accepted or downloaded.

## Delivery Phases

### Phase 1: Upload and lazy extraction

1. Add DOCX extraction dependency and content sniffing.
2. Refactor upload validation/extraction into format-specific handlers.
3. Preserve raw extracted text and add immutable source metadata.
4. Add extraction DTOs, prompt, version, status, and persistence.
5. Add the persisted analysis-job abstraction and worker lifecycle.
6. Add extraction retrieval/status APIs and focused service/controller tests.

### Phase 2: Two-step JD matching

1. Extract and categorize JD requirements.
2. Compare requirements with structured CV sections.
3. Return requirement judgments and evidence references.
4. Verify evidence against CV source blocks.
5. Calculate section scores and the overall score in backend code.
6. Replace the old synchronous match contract with the asynchronous typed result.

#### Progress

The typed two-step matching core is implemented. The backend now has typed requirement categories,
requirement match statuses, requirement-level evidence fields, and deterministic overall/section
score calculation. The match service calls separate requirement-extraction and requirement-matching
prompts and no longer trusts a model-provided overall score.

The remaining Phase 2 work is to make review and JD matching use the persisted job abstraction,
trigger structured extraction automatically when it has not completed, verify evidence against
source blocks, and expose the completed async result through the frontend contract.

The persisted JD-match job path is now implemented. JD matching returns `202 Accepted`, queues a
user-owned analysis job, ensures structured extraction before matching, and records the completed
analysis ID. Evidence quotes are checked against the stored raw CV text; positive unsupported
claims are downgraded to `UNCLEAR` before scoring.

SSE lifecycle delivery and frontend job-state integration remain outstanding. Source-block-specific
verification will be strengthened once matching consumes the structured extraction JSON directly.

#### Frontend progress

The frontend now accepts PDF and DOCX uploads, consumes the `202 Accepted` JD-match job response,
polls the ownership-scoped job status endpoint while work is queued or running, shows stage-based
status text, refreshes analysis history after completion, and renders section scores and requirement
judgments. It deliberately does not display a fabricated percentage or require a WebSocket.

The frontend integration is now build-clean. The duplicate Vitest `test` block was removed from
`vite.config.ts` because test configuration belongs in `vitest.config.ts`; the CV page also exposes
polling transport errors and uses the correct PDF/DOCX empty state.

### Phase 3: Evidence and ATS

1. Finalize evidence mapping fields and verification behavior.
2. Add separate ATS analysis type, prompt, endpoint, and DTO.
3. Implement deterministic ATS checks.
4. Add AI semantic ATS recommendations.
5. Persist and retrieve ATS results through the common analysis history.

### Phase 4: Rewrite and tailoring

1. Add immutable general rewrite suggestions.
2. Add JD-specific tailoring suggestions linked to requirements and evidence.
3. Enforce fact-preservation and unsupported-claim warnings.
4. Add derived CV/version persistence only when generated document behavior is specified.

## Quality Gates

Every phase must include:

- service unit tests with mocked `AiService`
- parser and prompt-variable tests
- ownership isolation tests
- controller validation and `ApiResponse` tests for new endpoints
- Flyway/JPA validation when schema changes land
- updated frontend API types whenever a backend DTO changes

No test calls the real Gemini API. AI confidence is never accepted without backend evidence
grounding where grounding is required.
