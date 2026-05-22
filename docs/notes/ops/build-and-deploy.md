---
title: Build & Deploy Reference
category: ops
created: 2026-04-27
status: stable
tags: [docker, shadow-cljs, pnpm, deployment]
---

# Build & Deploy Reference

## Quick Rebuild (from workspace root)

```bash
./orgs/open-hax/openplanner/packages/knoxx/backend/scripts/rebuild-image.sh
```

## Manual Steps

```bash
cd orgs/open-hax/openplanner/packages/knoxx/backend
pnpm install                          # install deps if needed
npx shadow-cljs release app           # compile CLJS
docker build -t knoxx-knoxx-backend:latest .
cd ../../../services/openplanner
docker compose up -d knoxx-backend
```

## Dev / Watch Mode

```bash
# Terminal 1
npx shadow-cljs watch app

# Terminal 2
docker compose up -d knoxx-backend
docker compose logs -f knoxx-backend
```

`dist/` is bind-mounted; the container picks up compiled changes automatically.
