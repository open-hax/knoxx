# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Also read **AGENTS.md** — it contains mandatory coding style rules, namespace conventions, verification requirements, modern CLJS patterns, and licensing doctrine that apply to all work in this repo.

## Project Overview

Knoxx is a local-first knowledge vault (named after Fort Knox) with a distributed architecture:

- **Backend** — ClojureScript / Node.js (Fastify HTTP + WebSocket, MCP server, Discord gateway)
- **Frontend** — React / TypeScript + ClojureScript (Vite + Shadow-cljs, TailwindCSS)
- **Ingestion** — Clojure / JVM (Ring/Jetty, file watching, pluggable driver pipeline)
- **Discord Bot** — TypeScript / Discord.js (optional)

## Commands

### Backend (ClojureScript, Node.js)

```bash
# From backend/
pnpm run watch        # Shadow-cljs watch (dev build)
pnpm run build        # Production build
pnpm start            # Run dist/server.js
pnpm start:dev        # Run dist-dev/server.js with reload
pnpm test             # Run tests (CI mode)
pnpm lint             # clj-kondo
pnpm typecheck        # shadow-cljs compile server (type check)
```

Run a single backend test file by compiling the test build and filtering by namespace:
```bash
pnpm -C backend exec shadow-cljs compile test
```
Any test failure — even with compiler exit 0 — is blocking (see AGENTS.md Verification Requirements).

### Frontend (React + ClojureScript)

```bash
# From frontend/
pnpm dev              # Start all dev watchers (Tailwind + Vite bridges + Shadow-cljs)
pnpm build            # Production build
pnpm test             # Vitest unit tests
pnpm test:watch       # Vitest watch mode
pnpm test:coverage    # Coverage report
pnpm test:e2e         # AVA end-to-end tests
pnpm typecheck        # TypeScript type check
```

### Ingestion (Clojure, JVM)

```bash
# From ingestion/
clj -M:dev            # Start nREPL + dev server (port 3002)
clj -M:test           # Unit tests (excludes translation pipeline)
clj -M:integration    # Integration tests (requires live OpenPlanner + DB)
clj -M:run            # Run server as main
clj -M:uberjar        # Build fat JAR
```

### Repository-Level

```bash
node scripts/lint-file-sizes.mjs   # File size budget check (warn 350, error 500 lines)
bash scripts/install-hooks.sh      # Install git hooks (run once per clone)
bash scripts/pre-push-checks.sh    # Run all pre-push checks manually
KNOXX_SKIP_PRE_PUSH=1 git push     # Skip pre-push checks (escape hatch)
```

Pre-push checks run: repo-wide size lint → backend clj-kondo → backend shadow-cljs compile → ingestion clj-kondo → frontend typecheck + size lint → discord-bot typecheck + size lint.

## Architecture

### Backend Domain Structure

The backend is organized into **vertical domain-driven slices** (see AGENTS.md for full rationale):

```
backend/src/cljs/knoxx/backend/
├── domain/          # Domain event handlers (music, media, models, etc.)
├── infra/agent/     # Agent orchestration (runner, runtime, session, message)
├── infra/           # HTTP, config, Redis, sessions
├── law/             # Policy & contract evaluation
├── shape/           # Type/shape definitions (TypeBox schemas)
└── tools/           # Tool implementations organized by domain
    ├── discord/
    ├── music/
    ├── openplanner/
    ├── contracts/
    ├── shared/      # Cross-domain helpers
    └── media/       # Media loading utilities
```

The orchestration layer (`infra/agent/`) composes domain tool vectors. Domain namespaces should never import each other — shared helpers live in `tools.shared` or `tools.media`.

### Frontend Multi-Build System

Three concurrent builds run in dev:
1. TailwindCSS → `dist/app.css`
2. Vite bridges → `dist/bridge/*.es.js` (TypeScript APIs exposed to ClojureScript)
3. Shadow-cljs → `dist/cljs/*.js`

The Vite dev server proxies `/api` and `/ws` to the backend.

### Ingestion Driver Pipeline

Pluggable drivers handle data sources (local filesystem, audio, images, scrapers, session logs). Each driver implements a common protocol (`browse`, `index`, `preview`). A passive file system watcher (Java WatchService) triggers scans automatically; sync intervals are configurable per source.

### Contracts & Authorization

Declarative contract files under `contracts/` define actors, roles, and policies. The backend loads and evaluates these at runtime to gate requests. The development actor is `pi@open-hax.local` (system-admin role). API key resolution uses the `X-API-Key` header.

## Key Environment Variables

```bash
# Backend
KNOXX_API_KEY=<key>
KNOXX_API_KEY_USER_EMAIL=pi@open-hax.local   # optional override

# Ingestion
OPENPLANNER_URL=http://localhost:7777
OPENPLANNER_API_KEY=<key>
WORKSPACE_PATH=/home/err/devel
MODELS_DIR=/path/to/gguf/models
```

## Hot Reload

Do **not** restart PM2/Knoxx processes unless the user explicitly asks. Shadow-cljs hot-reloads backend CLJS changes; Vite reloads frontend changes. If a restart seems necessary, report why and wait for approval.
