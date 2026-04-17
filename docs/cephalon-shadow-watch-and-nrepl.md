# Knoxx Cephalon: shadow-cljs watch + nREPL + PM2 dev loop

## Goal

When Knoxx is running under PM2 on the host, we still want:

- **shadow-cljs watch** running continuously (so CLJS changes compile immediately)
- **nREPL** exposed (shadow-cljs provides it; default port in `backend/shadow-cljs.edn` is `4500`)
- the **Node server process to restart** when the compiled CLJS output changes

This enables agents to iterate on Knoxx’s own runtime without manual rebuild/restart.

## What to run

### 1) CLJS compiler + nREPL

From:

- `orgs/open-hax/openplanner/packages/knoxx/backend`

Run:

```bash
pnpm watch
```

This starts `shadow-cljs watch app` and the shadow nREPL (see `shadow-cljs.edn`).

### 2) Node server with auto-restart

The CLJS output is written to `backend/dist/**`. Node **does not hot-reload** ESM imports automatically, so you need a restart loop.

Options:

- **Node built-in watcher** (simple, works well in dev):

```bash
node --watch src/server.mjs
```

- **PM2 watch** mode (restart on dist/src changes):

```bash
pm2 start src/server.mjs --name knoxx-cephalon-dev --watch --watch-delay 500
```

(Prefer `--ignore-watch` for logs/temp dirs if needed.)

## PM2 ecosystem example (safe template)

This repo does **not** ship your real host secrets. Instead, use an example ecosystem file and copy it to your host-local Knoxx config.

See:

- `orgs/open-hax/openplanner/packages/knoxx/backend/ecosystem.dev.example.cjs`

## nREPL

- Default port: `4500` (from `backend/shadow-cljs.edn`)
- The nREPL is provided by the `shadow-cljs watch` process.

From Emacs/CIDER you can connect to `localhost:4500` and then select the `:app` build.
