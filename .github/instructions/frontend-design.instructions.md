---
description: "Use when writing or modifying visual/UI code in fe/: CSS tokens, layout, color, typography, component styling, or dark mode."
applyTo: "fe/**"
---

# Frontend Design System

CareerPilot's visual direction is "Quiet Technical": neutral surfaces, one restrained accent,
monospace for numeric/status data, and no decoration that doesn't carry meaning. This is a tool
used under stress (job search, interview prep) with dense, decision-relevant content (scores,
skill gaps, live timers) — clarity and trust beat visual flourish. See
[docs/03-frontend-architecture.md](../../docs/03-frontend-architecture.md) for the full rationale.

## UI Generation Priority

When building a new page or component, in order:

1. Understand the user's intent.
2. Inspect existing UI patterns and reuse the existing design tokens/components.
3. Design the information hierarchy before writing markup.
4. Implement the simplest maintainable component structure.
5. Implement all required UI states (loading/success/empty/error/disabled).
6. Make the layout responsive (mobile/tablet/desktop/large desktop).
7. Verify the visual result; fix inconsistencies before finishing.

Do not optimize for "looks impressive." Optimize for clarity, hierarchy, consistency, and
usability.

## Design Tokens

Tokens live in `fe/src/styles/global.css` as CSS custom properties on `:root` (light, default)
and `:root[data-theme='dark']` (dark override, applied by `ThemeProvider`). Never hardcode a raw
color/spacing/radius value in component CSS when a token exists for it.

- **Typography**: `--font-sans` (Inter) for all UI text and headings; `--font-mono` (JetBrains
  Mono) only for numeric/status data — scores, countdown timers, session/record IDs, code blocks.
  There is no separate display/serif face; the same sans is used everywhere, including auth pages.
- **Color roles**: `--canvas` (page background), `--surface` (cards/panels/inputs), `--border`
  (1px hairlines), `--text`, `--text-muted`, `--accent`/`--accent-strong`/`--accent-soft` (single
  primary accent, indigo/blue), and semantic pairs `--success`/`--success-soft`,
  `--warning`/`--warning-soft`, `--danger`/`--danger-soft`, `--info`/`--info-soft`.
- **Semantic color mapping is fixed** across the app: `success` = matched/passed/completed,
  `warning` = missing-but-optional/cooldown/timeout, `danger` = destructive actions/expired/
  integrity flags, `info` = neutral status. Never use color alone to communicate state — pair it
  with an icon or text label.
- **Spacing**: 4/8/12/16/24/32/48px scale. **Radius**: `--radius-sm` (4px, inputs/buttons),
  `--radius-md` (8px, cards), full-pill only for tag/badge chips.
- **Shadow**: one elevation level (`--shadow-overlay`), used only for dropdowns/modals/popovers.
  Resting surfaces (cards, panels) use a 1px `--border`, never a shadow. No hard-offset "sticker"
  shadows, no gradients used decoratively.
- **Dark mode** is a first-class requirement, not a later addition. It defaults to the visitor's
  `prefers-color-scheme`, with a manual override persisted in `localStorage` via `ThemeProvider`/
  `useTheme` (`fe/src/app/providers/`). Every new color must have a value in both `:root` and
  `:root[data-theme='dark']` before it ships.

## Component Inventory

Reuse these before creating a one-off pattern: buttons (primary/secondary/ghost/destructive),
tag/badge chips (used for skill match/gap and score bands), inputs, cards, empty/loading/error
state blocks (`PageMessage` and its future siblings), and alerts. Add a new shared component only
after a second feature needs the same pattern.

## Surface-Specific Layout Guidance

- **CV Analysis**: card with a header row (file name, date), pill chips for matched (success) vs.
  missing (warning) skills, and the match score as a large monospace numeral — not a decorative
  gauge or progress ring.
- **Chat**: a compact conversation list plus a flat message list (no heavy bubble chrome),
  generous line-height, a sticky bottom composer, and monospace for rendered code blocks.
- **Interview**: a status header with a phase chip, a monospace countdown, and a connection-state
  indicator. Integrity events render as a quiet inline log — never a pulsing or alarming style,
  since they are observations, not accusations (see
  [docs/04-1-backend-interview.md](../../docs/04-1-backend-interview.md)).

## Anti-Patterns

Do not reintroduce: hard-offset "sticker" shadows, a second display/serif typeface, more than one
accent hue, decorative gradients, excessive rounded corners, or shadows on resting (non-overlay)
surfaces.
