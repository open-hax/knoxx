---
uuid: "knoxx-frontend-uxx-helix-native"
title: "Frontend: native uxx-helix consumption — retire vite.bridge.config.ts"
status: accepted
priority: P1
labels: ["tasks", "frontend", "helix", "uxx", "vite-retirement", "has-parent"]
created_at: "2026-06-11T00:00:00Z"
points: 3
category: tasks
---
# Frontend: native uxx-helix consumption — retire vite.bridge.config.ts

> Parent epic: `knoxx-frontend-helix-migration-vite-retirement`

## Why the bridge exists

`vite.bridge.config.ts` builds `dist/bridge/knoxx-frontend-bridge.es.js`,
re-exporting `@open-hax/uxx` React components (Markdown etc.), because:

- `@open-hax/uxx-helix` ships **compiled** CLJS with its own goog/cljs runtime
  — ESM-importing it into a shadow build collides ("Closure primitive methods
  must be called at file scope").
- `@open-hax/uxx` React Markdown's transitive deps (react-markdown →
  comma-separated-tokens etc.) aren't resolvable by shadow under pnpm's nested
  node_modules.

CLJS consumes the bridge via `:js-options :resolve {:target :file}`.

## Scope — eliminate the need

Preferred: put **uxx-helix CLJS source** on the shadow classpath (publish a
source artifact or `:local/root` / source npm subpath) so shadow compiles it
like any CLJS lib — one runtime, no ESM collision, node-testable. uxx source
lives at `~/devel/orgs/open-hax/uxx` (and uxx-helix ns is
`devel.ui.helix.core`).

Fallbacks if source consumption stalls:
- pnpm hoisting (`public-hoist-pattern`) or shadow `:resolve` entries to make
  react-markdown's dep tree resolvable, bundling via shadow instead of Vite;
- esbuild one-liner replacing the Vite config (lesser win — still a bridge,
  but Vite dies).

## Definition of done

- No CLJS namespace requires `@open-hax/knoxx-frontend-bridge`.
- `vite.bridge.config.ts` deleted; `build:bridge` script and the BRIDGE
  watcher removed from `dev`/`build`/`test:e2e` scripts.
- Markdown rendering verified in the running app (SourceDocPage forum view is
  the reference consumer).
- If source-consumption path chosen: a uxx-helix component is covered by a
  node cljs.test (links with `knoxx-frontend-helix-component-test-harness`).
