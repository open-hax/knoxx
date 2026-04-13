# Knoxx Backend

CLJS-based backend service for the Knoxx agent runtime.

## Architecture

```
src/
├── server.mjs        # JS bootstrap (Node.js entry point)
├── policy-db.mjs     # Policy database migrations
└── cljs/knoxx/backend/
    ├── core.cljs     # App initialization
    ├── app_routes.cljs  # HTTP routes
    ├── agent_runtime.cljs  # Agent execution
    └── ...

dist/
└── app.js            # Compiled CLJS output (shadow-cljs)
```

### JS Bootstrap Pattern

The backend uses a **JS bootstrap pattern** where:
- `server.mjs` is the Node.js entry point
- It imports compiled CLJS from `dist/app.js`
- This allows Node.js built-ins (fs, stream, etc.) to work correctly

**Why this pattern?**
- shadow-cljs `:target :esm` with `:js-provider :import` doesn't bundle Node built-ins
- The JS bootstrap imports packages with built-ins and passes them via runtime object
- CLJS code receives runtime objects and can use Node.js APIs

## Build Process

### Prerequisites

1. Java 11+ installed (for shadow-cljs)
2. Node.js 22+ with pnpm
3. Docker (for containerized deployment)

### Quick Rebuild

```bash
# From workspace root
./orgs/open-hax/openplanner/packages/knoxx/backend/scripts/rebuild-image.sh
```

### Manual Build Steps

```bash
# 1. Navigate to backend directory
cd orgs/open-hax/openplanner/packages/knoxx/backend

# 2. Install dependencies (if needed)
pnpm install

# 3. Compile CLJS
npx shadow-cljs release app

# 4. Build Docker image
docker build -t knoxx-knoxx-backend:latest .

# 5. Restart container
cd ../../../services/openplanner
docker compose up -d knoxx-backend
```

## Development Mode

For development, the docker-compose.yml uses bind mounts:
- `dist/` is bind-mounted into the container
- Compile locally with `npx shadow-cljs watch app`
- Container picks up changes automatically

```bash
# Terminal 1: Watch mode compilation
cd orgs/open-hax/openplanner/packages/knoxx/backend
npx shadow-cljs watch app

# Terminal 2: Start container
cd services/openplanner
docker compose up -d knoxx-backend
docker compose logs -f knoxx-backend
```

## shadow-cljs Configuration

See `shadow-cljs.edn` for build configuration.

Key settings:
- `:target :esm` - Output ES modules
- `:js-provider :import` - Import Node.js packages at runtime
- `:output-dir "dist"` - Compiled output directory
- `:modules {:app {:exports {}}}` - Single module with all code

## Dockerfile

The Dockerfile:
1. Uses `node:22-bookworm-slim` base image
2. Copies `dist/` and `src/server.mjs`
3. Installs production dependencies only
4. Runs as non-root user (UID 1000)

## Troubleshooting

### "fs not available" error

This happens when shadow-cljs tries to bundle Node.js built-ins.
Solution: Use `:js-provider :import` and ensure server.mjs imports packages with built-ins.

### Container crash on startup

Check the logs:
```bash
docker compose logs knoxx-backend --tail=50
```

Common causes:
- Missing `dist/app.js` - compile CLJS first
- Syntax error in CLJS - check shadow-cljs output
- Missing dependency - run `pnpm install`

### Image not updating

If the container doesn't pick up changes:
```bash
# Force rebuild with no cache
docker build --no-cache -t knoxx-knoxx-backend:latest .
docker compose up -d knoxx-backend --force-recreate
```
