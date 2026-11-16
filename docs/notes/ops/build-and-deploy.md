---
title: Build & Deploy Reference
category: ops
created: 2026-04-27
status: stable
tags: [docker, shadow-cljs, pnpm, deployment]
---

# Build & Deploy Reference

> **Updated 2026-09-30:** Knoxx is now its own repository (`open-hax/knoxx`), not
> `orgs/open-hax/openplanner/packages/knoxx`. The backend runtime build is
> `:server` (`backend/shadow-cljs.edn:91-95`, output `dist/server.js`); the Docker
> image runs `node dist/server.js` (`backend/Dockerfile:48`). The old `:app` build
> still exists (`backend/shadow-cljs.edn:9-11`) but is not the container entrypoint.

## Hosted deployments

Use [Deployment ownership](../../../README.md#deployment-ownership) for
the Services workflow and exact source revision. The commands below
operate on a workstation checkout.

## Quick local rebuild

From the Knoxx repository root, run:

```bash
backend/scripts/rebuild-image.sh
```

The script builds the image named by `KNOXX_BACKEND_IMAGE`, which defaults to
`knoxx-backend:latest`. For a workstation Compose project, set
`KNOXX_BACKEND_COMPOSE_FILE` to its local file and
`KNOXX_BACKEND_COMPOSE_SERVICE` to its backend service.

## Manual local build

```bash
cd backend
pnpm install
pnpm run build
docker build -t knoxx-backend:latest .
```

## Dev / Watch Mode

```bash
cd backend
pnpm run watch        # shadow-cljs watch server-dev -> dist-dev/server.js
pnpm run start:dev    # nbb scripts/start-server-dev.cljs (waits for dist-dev, then imports it)
```

See `nrepl-pm2-shadow-setup.md` for the PM2 dev loop. The earlier claim that
`dist/` is bind-mounted into a container for hot reload is not backed by any
compose file in this repository (2026-09-30).
