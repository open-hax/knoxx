---
title: Troubleshooting Guide
category: ops
created: 2026-04-27
status: stable
tags: [debug, docker, shadow-cljs, errors]
---

# Troubleshooting Guide

## `fs not available` error

shadow-cljs is trying to bundle Node.js built-ins.  
**Fix:** Use `:js-provider :import` and ensure `server.mjs` imports packages
with built-ins and injects them into CLJS.
(2026-09-30: there is no `server.mjs` any more; `backend/shadow-cljs.edn:7` sets
`:js-provider :import` globally and the `:server`/`:server-dev` builds list Node
built-ins and native packages under `:keep-as-import`, e.g. `shadow-cljs.edn:60-70`.)

## Container crash on startup

```bash
docker compose logs knoxx-backend --tail=50
```

Common causes:
- Missing `dist/app.js` — compile CLJS first (2026-09-30: the container now runs
  `dist/server.js`, built by `pnpm run build` / `shadow-cljs release server`;
  see `backend/Dockerfile:48`)
- Syntax error in CLJS — check shadow-cljs output
- Missing dependency — run `pnpm install`

## Image not updating

```bash
docker build --no-cache -t knoxx-backend:latest .   # 2026-09-30: default image name per backend/scripts/rebuild-image.sh
docker compose up -d knoxx-backend --force-recreate
```

## `cljs$core$IFn$_invoke$arity$4` crash

**Root cause:** shadow-cljs `:optimizations :simple` bug where local bindings
named with `!` suffix get incorrectly compiled as namespace property references.

**Symptoms:** Container crashes on startup; error references a `!`-suffixed function;
error is in a file where that function is passed through a destructured map.

**Fix:** Import the function directly via `:refer [function!]` instead of passing
it through a map parameter.

```clojure
;; BEFORE (buggy)
(defn register-routes! [app config {:keys [route! ...]}]
  (route! app ...))

;; AFTER (fixed)
(ns knoxx.backend.my-routes
  (:require [knoxx.backend.app-shapes :refer [route!]]))
;; 2026-09-30: the namespace is now knoxx.backend.shape.app-shapes
;; (backend/src/cljs/knoxx/backend/shape/app_shapes.cljs:1, route! at :259)
(defn register-routes! [app config {:keys [...]}]
  (route! app ...))
```

See commit `96f76d41` for the canonical fix.

## Inspect compiled output

```bash
grep -n 'route_BANG_' dist/app.js | head -20   # 2026-09-30: inspect dist/server.js for the runtime build
```
Look for namespace property references like `knoxx.backend.X.route_BANG_`
instead of local variable references.
