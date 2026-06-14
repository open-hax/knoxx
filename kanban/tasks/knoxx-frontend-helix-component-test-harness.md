---
uuid: "knoxx-frontend-helix-component-test-harness"
title: "Frontend: Helix component unit-test harness (kill the 'Helix can't be unit tested' myth)"
status: accepted
priority: P1
labels: ["tasks", "frontend", "helix", "testing", "has-parent"]
created_at: "2026-06-11T00:00:00Z"
points: 3
category: tasks
---
# Frontend: Helix component unit-test harness

> Parent epic: `knoxx-frontend-helix-migration-vite-retirement`

## Background — the actual constraint

"Helix can't be unit tested" is false. Verified 2026-06-11: `pnpm test:cljs`
(shadow `:test` build, `:node-test` target) runs 29 tests / 127 assertions
green. Helix itself (`lilactown/helix`) is a classpath CLJS lib and React is
CJS-compatible, so Helix **components** can render in node tests via
`react-dom/server` `renderToStaticMarkup` (or RTL + jsdom).

The real, narrow constraint: the `:node-test` target emits **CommonJS**
(`dist/test/frontend-tests.cjs` — required because the harness uses
`__dirname`), and CJS cannot synchronously require **ESM-only npm packages**.
`@open-hax/uxx-helix` is ESM-only (`exports {".": {"import": ...}}` with no
CJS entry), so only components that import uxx-helix are blocked — and that is
a packaging problem, not a Helix problem.

## Scope

1. Add a component test to the existing `:test` build proving a plain Helix
   `defnc` renders under node (e.g. `renderToStaticMarkup` assertion on a
   migrated view-free component). This documents the baseline capability.
2. Solve the ESM-only-dep case, picking one:
   - a second shadow test build with `:target :esm` + a small ESM test runner
     (node `--test` or a hand-rolled cljs.test main), able to import uxx-helix;
   - or ship a CJS export from uxx-helix itself (fix at the package);
   - or accept the architecture rule (pure logic uxx-free + thin views, views
     verified by app build + ava e2e) and write it down as the decided policy.
3. Document the outcome in `frontend/AGENTS.md` (or equivalent) so agents stop
   re-deriving "Helix can't be unit tested".

## Progress

- 2026-06-11: Scope item 1 DONE.
  `test/cljs/knoxx/frontend/admin/event_agent_components_render_test.cljs`
  renders production Helix components in the `:node-test` build via
  `react-dom/server` `renderToStaticMarkup`: `badge`, `status-badge`
  (conditional tones), `collapsible-panel` (children + open attr), and
  `event-dispatch` (3× `hooks/use-state` — proves hook-using components
  render with initial state). `pnpm test:cljs`: 33 tests / 145 assertions,
  0 failures, 0 warnings. clj-kondo clean on the test file.
  Honest boundary: this proves *render* testing (initial output HTML).
  Interaction testing (events, post-render state transitions) is NOT yet
  proven — would need jsdom + RTL or react-test-renderer in the node build.
  The ESM-only-dep constraint (uxx-helix) remains real and open (scope 2).

## Definition of done

- A green test exists that renders a Helix component (not just pure logic).
- The ESM-only-dep decision is implemented or explicitly recorded as policy.
- `pnpm test:cljs` (and any new test script) green; wired into
  `scripts/pre-push-checks.sh` if a new script is added.
