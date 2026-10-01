---
title: "nREPL + PM2 + shadow-cljs Watch Setup"
category: ops
created: 2026-04-24
original: 2026.04.24.09.38.51.md
status: note
---

> **Update 2026-09-30:** The PM2 host wiring no longer lives in this repository.
> Root `ecosystem.config.cjs:1-30` is a deprecated shim that throws unless
> `KNOXX_HOST_ECOSYSTEM_CONFIG` points at the host config (documented as
> `services/openplanner/ecosystem.host.config.cjs`, not present in this checkout,
> so the live PM2 process names are unverified here). The in-repo template
> `backend/ecosystem.dev.example.cjs:19-35` names the processes
> `knoxx-shadow-watch` (`pnpm exec shadow-cljs --source-maps watch server-dev`) and
> `knoxx-backend-dev`, and starts the backend through
> `nbb scripts/start-server-dev.cljs`, which waits for and imports
> `dist-dev/server.js` (`backend/scripts/start-server-dev.cljs:15`);
> `node dist-dev/server.js` remains available as `pnpm start:dev:direct`
> (`backend/package.json:14`). nREPL port `4500` is confirmed at
> `backend/shadow-cljs.edn:6`; `9630` is shadow-cljs's default server port (no
> override in `shadow-cljs.edn`). `knoxx-shadow` / `knoxx-backend` below are the
> names used at the time of writing.

## Current model

Knoxx uses a two-process dev loop under PM2:

1. `knoxx-shadow` runs `shadow-cljs --source-maps watch server-dev`.
2. `knoxx-backend` runs `node --enable-source-maps dist-dev/server.js`.

The watched `:server-dev` build writes to `backend/dist-dev/**` and includes the shadow Node ESM devtools client. The Node process owns Fastify/backend runtime and connects back to the shadow websocket.

The `:server` build writes to `backend/dist/**` and is reserved for compile/release verification. Do not run the PM2 dev backend from `dist/server.js`.

## What happens when the dev loop is healthy

1. The shadow JVM launches and starts nREPL on port `4500`.
2. The `:server-dev` build compiles to `dist-dev/server.js`.
3. PM2 starts the Node backend from `dist-dev/server.js` after the dev output exists.
4. The Node runtime imports the shadow Node ESM devtools client and connects to the shadow websocket.
5. Editor/nREPL evals selected against `:server-dev` can reach the live CLJS runtime.

Check the runtime connection with:

```clojure
(require '[shadow.cljs.devtools.api :as shadow])
(shadow/repl-runtimes :server-dev)
;; should be non-empty
```

## Why `:server-dev` exists

Running `shadow-cljs compile server` or `shadow-cljs release server` can overwrite `dist/server.js` with non-hot-reload output. If PM2 also uses that path, a backend restart can silently lose the devtools websocket connection.

Separating outputs keeps the contracts explicit:

| Build | Output | Purpose |
|---|---|---|
| `:server-dev` | `dist-dev/` | PM2 hot-reload runtime |
| `:server` | `dist/` | compile/release verification |

## PM2 constraints

- Keep `knoxx-backend.watch = false`; PM2 dist watching fights the shadow hot-reload model and can create restart storms.
- Start/keep `knoxx-shadow` before `knoxx-backend` so the backend loads watch-produced dev output and can connect to port `9630`.
- If the shadow JVM restarts, verify `repl-runtimes :server-dev`; restart `knoxx-backend` if it did not reconnect cleanly.
