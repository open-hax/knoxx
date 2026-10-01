---
title: Knoxx Backend — Architecture Overview
category: architecture
created: 2026-04-27
status: stable
tags: [cljs, shadow-cljs, node, esm, docker]
---

# Knoxx Backend — Architecture Overview

> **Historical (banner added 2026-09-30).** This describes the retired JS-bootstrap
> backend (`src/server.mjs`, `src/policy-db.mjs`, `dist/app.js`), none of which
> exists now. The current runtime is all-CLJS: `:server`/`:server-dev` builds with
> `knoxx.backend.entrypoint/init` -> `knoxx.backend.bootstrap/bootstrap!`. See
> `backend/README.md`. The `status: stable` frontmatter is left as authored.

CLJS-based backend service for the Knoxx agent runtime, compiled by shadow-cljs
to an ESM bundle and served by a Node.js bootstrap.

## Source Layout

```
src/
├── server.mjs                    # JS bootstrap (Node.js entry point)
├── policy-db.mjs                 # Policy database migrations
└── cljs/knoxx/backend/
    ├── core.cljs                 # App initialization
    ├── app_routes.cljs           # HTTP route registration
    ├── agent_runtime.cljs        # Agent execution loop
    └── ...                       # See module index

dist/
└── app.js                        # Compiled CLJS output
```

## JS Bootstrap Pattern

The backend uses a **JS bootstrap pattern** where `server.mjs` is the Node.js
entry point. It imports compiled CLJS from `dist/app.js` and injects Node.js
packages that shadow-cljs cannot bundle under `:js-provider :import`.

**Why:** shadow-cljs `:target :esm` with `:js-provider :import` does not bundle
Node built-ins. The bootstrap imports them and passes them to CLJS via a runtime
object, giving CLJS access to `fs`, `stream`, `child_process`, etc.

## shadow-cljs Configuration

Key `shadow-cljs.edn` settings:

| Key | Value | Reason |
|---|---|---|
| `:target` | `:esm` | ES module output |
| `:js-provider` | `:import` | Node built-ins at runtime |
| `:output-dir` | `"dist"` | Compiled output location |
| `:modules` | `{:app {:exports {}}}` | Single module, all code |

## Dockerfile

- Base: `node:22-bookworm-slim`
- Copies `dist/` + `src/server.mjs`
- Installs production deps only
- Runs as non-root user (UID 1000)
