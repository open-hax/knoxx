---
uuid: "knoxx-frontend-vitest-retirement"
title: "Frontend: port remaining test value to cljs.test — remove vite/vitest deps"
status: accepted
priority: P2
labels: ["tasks", "frontend", "testing", "vite-retirement", "has-parent"]
created_at: "2026-06-11T00:00:00Z"
points: 3
category: tasks
---
# Frontend: port remaining test value to cljs.test — remove vite/vitest deps

> Parent epic: `knoxx-frontend-helix-migration-vite-retirement`
> Gated on: `knoxx-frontend-app-bridge-retirement` (TS sources gone),
> `knoxx-frontend-uxx-helix-native` (bridge gone)

## Background

vitest currently runs 217 passing / 41 todo tests over the TS sources and the
loader shims. As TS modules migrate, their vitest tests are ported to
cljs.test and deleted with the module (already the pattern — 29 cljs tests
cover the six migrated libs). This card is the endgame sweep.

## Scope

- Audit remaining vitest suites; every test either (a) already has a cljs.test
  port, (b) gets ported with its module's migration card, or (c) is shim-only
  contract testing that dies with the shims. Nothing silently dropped — record
  the disposition list on this card.
- Delete `vitest.config.ts`, `src/test/` setup, `test`/`test:watch`/
  `test:coverage` scripts; `test` becomes the cljs gate.
- Remove `vite`, `vitest`, `@vitest/coverage-v8`, `@vitejs/plugin-react`,
  `@testing-library/*`, `jsdom` (unless the Helix component harness adopted
  it), `typescript`, `@types/react*` from package.json once nothing needs them.
- `dev` script reduces to tailwind + shadow-cljs watch; `build` to bridge-free
  shadow release + tailwind.
- Update `scripts/pre-push-checks.sh` and CI: frontend gate = clj-kondo +
  shadow compile + `test:cljs` (+ e2e), no tsc/vitest.

## Definition of done

- `grep -r vite frontend/package.json` empty; `pnpm dev`, `pnpm build`,
  `pnpm test:cljs`, `pnpm test:e2e` all green without Vite installed.
- Disposition list for every retired vitest suite recorded here.
