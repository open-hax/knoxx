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

## Quick Rebuild (from the Knoxx repository root)

```bash
backend/scripts/rebuild-image.sh
# optional compose restart:
KNOXX_BACKEND_COMPOSE_FILE=/path/to/compose.yml \
KNOXX_BACKEND_COMPOSE_SERVICE=knoxx-backend \
  backend/scripts/rebuild-image.sh
```

The script builds image `${KNOXX_BACKEND_IMAGE:-knoxx-backend:latest}` and only
restarts compose when `KNOXX_BACKEND_COMPOSE_FILE` is set
(`backend/scripts/rebuild-image.sh:1-20`).

## Manual Steps

```bash
cd backend
pnpm install                          # install deps if needed
pnpm run build                        # shadow-cljs release server -> dist/server.js
docker build -t knoxx-backend:latest .
# then restart the knoxx-backend service in whichever compose file deploys it,
# e.g. services/digitalocean/services/knoxx/compose.yaml in the Foresight
# services repository (service `knoxx-backend`, image from KNOXX_BACKEND_IMAGE).
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
