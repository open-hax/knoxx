# Knoxx Cephalon: shadow-cljs watch + nREPL + PM2 dev loop

## Goal

When Knoxx is running under PM2 on the host, the dev loop is intentionally two-process:

- `knoxx-shadow`: `shadow-cljs --source-maps watch server-dev`
- `knoxx-backend`: `node --enable-source-maps dist-dev/server.js`

The watched `:server-dev` build writes to `backend/dist-dev/**` and includes the shadow Node ESM devtools client. The Node runtime must load that output so it can connect to the shadow websocket and receive hot reloads.

`backend/dist/**` belongs to the non-hot-reload `:server` build and is reserved for compile/release verification.

## What to run

Prefer the checked-in PM2 ecosystem from the Knoxx root:

```bash
pm2 start ecosystem.config.cjs --only knoxx-shadow,knoxx-backend
```

*2026-09-30:* the root `ecosystem.config.cjs` is now a deprecated shim that
throws unless `KNOXX_HOST_ECOSYSTEM_CONFIG` points at a host ecosystem file
(see the root `README.md`). The current `knoxx-backend` process runs
`nbb scripts/start-server-dev.cljs` (`pnpm -C backend run start:dev`), which
waits for the watch artifact and then imports `dist-dev/server.js`.
`backend/scripts/dev-watch.sh` is a single-terminal helper that starts both.

Manual foreground runs from `backend/` are only for temporary debugging. The canonical local dev supervisor is PM2; do not use wrapper scripts that background shadow and Node together.

For verification only:

```bash
pnpm typecheck  # shadow-cljs compile server, writes dist/
pnpm build      # shadow-cljs release server, writes dist/
```

## Invariants

- Do not enable PM2 `watch` on `knoxx-backend`; shadow-cljs owns hot reload.
- Do not point the dev runtime at `dist/server.js`; compile/release verification can overwrite it with non-devtools output.
- A healthy live loop has a connected runtime:

```clojure
(shadow.cljs.devtools.api/repl-runtimes :server-dev)
;; => non-empty
```

## nREPL

- Default port: `4500` from `backend/shadow-cljs.edn`.
- The nREPL is provided by the `knoxx-shadow` / `pnpm watch` shadow process.
- Select the `:server-dev` build for live CLJS eval into the connected Node runtime.
