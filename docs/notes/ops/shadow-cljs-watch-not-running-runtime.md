---
title: "shadow-cljs Watch ≠ Node Runtime — Root Finding"
category: ops
created: 2026-04-24
original: 2026.04.24.10.04.56.md
status: note
---

## Signal

`shadow-cljs watch` is the compiler/control plane; it is not sufficient by itself to prove the Knoxx backend runtime is hot-reload-connected.

For the current ESM backend dev loop, Knoxx needs:

1. a shadow JVM watching `:server-dev`, and
2. a separate Node process loading `dist-dev/server.js`.

The Node process must successfully connect back to the shadow websocket. The canonical health check is:

```clojure
(require '[shadow.cljs.devtools.api :as shadow])
(shadow/repl-runtimes :server-dev)
;; healthy => non-empty
```

## Current architecture

- `:server-dev`
  - output: `backend/dist-dev/**`
  - PM2 shadow command: `pnpm exec shadow-cljs --source-maps watch server-dev`
  - PM2 backend command: `node --enable-source-maps dist-dev/server.js`
  - includes devtools/autoload hooks for hot reload
- `:server`
  - output: `backend/dist/**`
  - used by `pnpm typecheck` / `shadow-cljs compile server` and `pnpm build` / `shadow-cljs release server`
  - not the PM2 dev runtime path

## Evidence pattern for a broken loop

A broken loop usually looks like one or more of these:

- `(shadow/repl-runtimes :server-dev)` returns `[]`.
- The backend was restarted from `dist/server.js` instead of `dist-dev/server.js`.
- PM2 logs show websocket disconnects, `ECONNREFUSED 127.0.0.1:9630`, stale output, or unexpected websocket responses.
- A verification compile overwrote `dist/server.js`, and PM2 was still pointed at `dist/server.js`.

## Countermoves

- Do not enable PM2 file watch on `knoxx-backend`.
- Do not point PM2 dev runtime at `dist/server.js`.
- Do not treat a successful `shadow-cljs compile server` as proof that the live runtime is connected.
- Do not use `node-repl` as the backend runtime; it is useful for scratch evaluation, not the app process.

## Recovery

1. Ensure `knoxx-shadow` is watching `server-dev`.
2. Ensure `backend/dist-dev/server.js` exists and imports the shadow Node ESM devtools client.
3. Restart `knoxx-backend` from `dist-dev/server.js`.
4. Re-check `(shadow/repl-runtimes :server-dev)`.
