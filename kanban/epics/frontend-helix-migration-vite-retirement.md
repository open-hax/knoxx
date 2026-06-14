---
uuid: "knoxx-frontend-helix-migration-vite-retirement"
title: "Frontend: finish TS→Helix migration and retire Vite entirely"
status: accepted
priority: P1
labels: ["epics", "frontend", "helix", "vite-retirement"]
created_at: "2026-06-11T00:00:00Z"
points: null
category: epics
---
# Frontend: finish TS→Helix migration and retire Vite entirely

Date: 2026-06-11
Status: accepted
Repo: `packages/agents/knoxx` (frontend/)

## Goal

All frontend UI is Helix CLJS compiled by shadow-cljs. Vite, vitest,
`@vitejs/plugin-react`, and all TS/TSX sources are removed from `frontend/`.
Lint unifies on clj-kondo.

## Current state (verified 2026-06-11)

- Source counts: **129 .tsx + 76 .ts vs 22 .cljs** (~12% migrated).
- The Vite **dev server is already gone** — shadow `:dev-http` on port 5173 is
  the dev entrypoint (serves `dist/`, proxies `/api`,`/ws` to :8000). The
  root-level CLAUDE.md claim that "Vite proxies /api and /ws" is stale.
- Vite survives only as a **library bundler**, in two configs:
  1. `vite.bridge.config.ts` → `dist/bridge/knoxx-frontend-bridge.es.js`:
     re-exports `@open-hax/uxx` React components (Markdown etc.) because
     shadow-cljs cannot resolve react-markdown's transitive deps under pnpm's
     nested node_modules.
  2. `vite.app-bridge.config.ts` → `dist/bridge/knoxx-app-bridge.es.js`:
     stable ESM surface over the legacy TS app, consumed by shadow via
     `:js-options :resolve {:target :file}`.
  Plus **vitest** for the TS test suite (217 pass / 41 todo).
- CLJS unit testing **works today**: shadow `:test` build (`:node-test` →
  `dist/test/frontend-tests.cjs`), `pnpm test:cljs` = 29 tests / 127
  assertions green. The claim "Helix can't be unit tested" is **false**; see
  the test-harness task for the actual (narrow) constraint.

## Migration pattern (established, keep using)

- Views: loader shim — `.tsx` becomes a thin wrapper lazy-loading the compiled
  CLJS from `window.knoxx.frontend.<ns>.<export>` (loading state → loud error
  after 1.5s → mount via error boundary; never silent TS fallback). Reference:
  `src/pages/SourceDocPage.tsx` + `src/cljs/knoxx/frontend/pages/source_doc/view.cljs`.
- Pure utils: migrate util + consumers together (sync imports can't use the
  async shim); port to CLJS + cljs.test, then delete the TS util + vitest test.
- Architecture rule: pure logic in uxx-free namespaces (node-testable); Helix
  view components thin.

## Child tasks

- `knoxx-frontend-helix-component-test-harness` — prove/standardize Helix
  component unit tests; ESM-capable test build.
- `knoxx-frontend-uxx-helix-native` — native uxx-helix consumption; retires
  `vite.bridge.config.ts`.
- `knoxx-frontend-app-bridge-retirement` — migrate remaining bridge-exposed TS
  surfaces; retires `vite.app-bridge.config.ts`.
- `knoxx-frontend-vitest-retirement` — port remaining test value to cljs.test;
  remove vite/vitest deps and scripts.

## Definition of done

- `frontend/package.json` has no `vite`, `vitest`, `@vitejs/plugin-react`
  dependencies; `dev` script is tailwind + shadow-cljs only.
- `vite.bridge.config.ts`, `vite.app-bridge.config.ts`, `vitest.config.ts`
  deleted; `src/bridge/` deleted.
- Zero `.tsx`/`.ts` app sources under `frontend/src` (ambient `.d.ts` may
  remain if still needed by tooling).
- `pnpm build` = shadow-cljs release + tailwind only; app verified in browser.
- `pnpm test:cljs` green and covering the logic the vitest suite used to cover.
