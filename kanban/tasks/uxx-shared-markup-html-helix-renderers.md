---
uuid: "uxx-shared-markup-html-helix-renderers"
title: "Define a shared markup AST with HTML and Helix renderers"
status: ready
priority: "P1"
labels: ["tasks", "frontend", "backend", "helix", "ssr", "uxx", "8sp"]
created_at: "2026-08-05T14:30:00Z"
source: "epics/frontend-helix-migration-vite-retirement.md"
points: 8
category: "tasks"
---
# Define a shared markup AST with HTML and Helix renderers

> Parent epic: `knoxx-frontend-helix-migration-vite-retirement`

## Context

Knoxx currently has two markup models:

- browser UI is moving toward Helix/React components;
- server-owned documents are assembled as escaped HTML strings.

The immediate example is
`backend/src/cljs/knoxx/backend/infra/routes/mcp/consent.cljs`. The page is now
isolated from OAuth policy and I/O, but it still owns a local `escape` function
and builds the whole document through nested `str` calls. That approach is hard
to review, easy to drift from frontend conventions, and will be repeated for
future consent, login, error, admin, email, and static-rendering surfaces.

Standardize the document model, not the runtime. A pure, Hiccup-shaped markup
AST should be the canonical representation. A safe HTML renderer will serve
Node/server/static use cases without React. A Helix adapter will turn the same
AST into React elements for browser rendering, SSR, and hydration boundaries.
The backend must not acquire a React dependency.

The first implementation may live in a Knoxx-local shared source package, but
its namespace, data contract, and public API must be extractable into
`@open-hax/uxx` without changing call sites.

## Proposed contract

The supported node grammar is intentionally small and validated:

```clojure
[:tag attrs child ...]
[:<> child ...]
[:raw-html trusted-value]
```

- Tags are keywords or approved strings.
- Attributes are maps with deterministic normalization.
- Children may be scalar text, nodes, or nested sequences.
- Components are ordinary pure functions returning nodes.
- Raw HTML is impossible without an explicit trusted wrapper.

The shared AST and HTML renderer should be portable CLJC/pure CLJS. The Helix
renderer may remain CLJS-only and depend on Helix/React.

## Work

1. **Record the architecture boundary**
   - Add a short ADR or design note defining the AST, renderer responsibilities,
     extraction path into UXX, and why the backend does not render through React.
   - Declare that business rules and request/database access produce normalized
     view models before markup functions run.

2. **Create a shared markup source package**
   - Add one source root consumable by both backend and frontend shadow builds.
   - Define constructors/predicates for elements, fragments, and trusted raw
     content.
   - Reject malformed nodes with useful errors in development and tests.

3. **Implement the safe HTML renderer**
   - Escape text and attribute values by default.
   - Render void elements correctly.
   - Normalize class values from strings, keywords, collections, and conditional
     entries.
   - Render boolean attributes consistently.
   - Produce deterministic attribute ordering for stable tests and caching.
   - Validate URL-bearing attributes (`href`, `src`, form `action`) against an
     explicit scheme policy.
   - Reject browser event attributes and function values.
   - Require an explicit trusted value for `:raw-html`.

4. **Implement the Helix renderer**
   - Convert the shared AST to React elements without changing component input.
   - Normalize HTML attribute names to React equivalents only in this adapter.
   - Preserve keys where supplied and provide useful warnings for generated
     sibling collections without keys.
   - Keep browser event handlers out of the portable server-safe subset; allow
     interactive Helix components to wrap or extend portable markup explicitly.

5. **Migrate the MCP consent page as the first fixture**
   - Refactor
     `backend/src/cljs/knoxx/backend/infra/routes/mcp/consent.cljs` so pure view
     functions return the shared AST.
   - Render the route response through the HTML renderer.
   - Remove the page-local `escape` function and bulk HTML string assembly.
   - Preserve all current fields, actor warning behavior, selected tool state,
     OAuth parameters, form method/action, styles, and visible copy.

6. **Add parity and security tests**
   - Cover escaping, fragments, nested sequences, class normalization, boolean
     attributes, void elements, deterministic output, unsafe URL rejection,
     event-attribute rejection, and explicit raw HTML.
   - Render one shared fixture through HTML and Helix/static React rendering and
     compare normalized DOM structure rather than byte-for-byte serialization.
   - Keep the existing MCP HTTP authorization/confirmation tests green and add
     assertions that hostile client IDs, redirect URIs, actor IDs, tool names,
     labels, and descriptions cannot inject markup.

## Non-goals

- Reimplementing React lifecycle, hooks, context, or reconciliation.
- Requiring React to render server-owned protocol pages.
- Building a general-purpose template language or macro system.
- Introducing CSS-in-JS as part of this task.
- Allowing arbitrary strings to bypass escaping.
- Migrating every existing frontend component in the first slice.

## Affected areas

- `backend/src/cljs/knoxx/backend/infra/routes/mcp/consent.cljs`
- backend and frontend `shadow-cljs.edn` source roots
- new shared markup AST and HTML renderer namespaces
- new frontend Helix adapter namespace
- backend renderer/consent tests
- frontend renderer parity tests
- architecture documentation

## Definition of Done

- Backend and frontend builds consume one canonical markup AST implementation.
- The backend HTML renderer has no React or browser runtime dependency.
- The Helix adapter renders the same portable AST into React elements.
- MCP consent markup is represented as AST and rendered through the shared HTML
  renderer; its local `escape` helper and large concatenated template are gone.
- All dynamic consent values are escaped and unsafe URL/event/raw-HTML paths are
  covered by negative tests.
- A normalized-DOM parity test passes for the representative shared fixture.
- `pnpm -C backend lint` and the relevant backend test suite pass.
- `pnpm -C frontend test:cljs` passes.
- The design note documents the future extraction boundary into
  `@open-hax/uxx` without requiring caller rewrites.

---
Triage 2026-08-05: Ready P1 architecture task. The current string-rendered MCP
consent page provides a bounded first fixture, while the AST/renderer split
prevents the server from depending on React and gives Helix a shared document
model. Security and parity acceptance criteria are concrete; no external blocker
is required before implementation. Verdict: ready (P1, 8sp).
---
