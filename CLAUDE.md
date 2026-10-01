# CLAUDE.md

> **Roadmap:** [`ROADMAP.md`](ROADMAP.md) — this repo's slice. The hub, with the
> seam, ownership table and sequencing rule, is [eta-mu/ROADMAP.md](https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md).

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Also read **AGENTS.md** — it contains mandatory coding style rules, namespace conventions, verification requirements, modern CLJS patterns, runtime-operation rules, and PR review/merge ownership that apply to all work in this repo.

## Project Overview

Knoxx is a local-first knowledge vault (named after Fort Knox) with a distributed architecture:

- **Backend** — ClojureScript / Node.js (Fastify HTTP + WebSocket, MCP server, Discord gateway)
- **Frontend** — React / TypeScript + ClojureScript (Vite + Shadow-cljs, TailwindCSS)
- **Ingestion** — Clojure / JVM (Ring/Jetty, file watching, pluggable driver pipeline)
- **Discord** — handled in-process by the backend Discord gateway (`domain/discord/`, `extern/discord`). The standalone TypeScript `discord-bot/` package was removed on 2026-05-21 (`b3348eb1`).

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

Compile and run the backend `:test` build (the whole cljs.test suite; there is no namespace filter on this command):
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
clj -M:dev            # Start nREPL with CIDER middleware (no HTTP server; port chosen by nREPL)
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

### Human verification

```bash
scripts/verify-publication-epic.sh   # API journey against a live Knoxx
scripts/verify-publication-tour.sh   # Browser tour + screenshots
```

Every epic ships a pair like this. See **AGENTS.md → Human Verification
Artifact** for what is required, and `docs/verification/` for the walkthroughs.

Pre-push checks run: repo-wide size lint → backend clj-kondo → backend shadow-cljs compile → ingestion clj-kondo → frontend size lint + typecheck → frontend changed-surface CLJS lint → frontend migration manifest check → discord-bot size lint + typecheck. (As of 2026-09-30 `scripts/pre-push-checks.sh` still runs the two discord-bot checks even though `discord-bot/` no longer exists, so they fail.)

## Architecture

### Backend Domain Structure

The backend is organized into **vertical domain-driven slices** (see AGENTS.md for full rationale):

```
backend/src/cljs/knoxx/backend/
├── domain/          # Domain logic; per-domain agent tools live in domain/<name>/tools.cljs
│                    #   (discord, voice, actor, openutau, policy, event, contracts, ...)
├── extern/          # The only home of raw JS interop (fastify, fetch, mongo, discord, ...)
├── infra/agent/     # Agent orchestration (runner, runtime, session, message, hydration)
├── infra/           # HTTP routes, config, Mongo stores, clients, lifecycle
├── law/             # Policy & contract evaluation
├── shape/           # Structure-only data shapes (plain CLJS/CLJC)
├── runtime/         # Process-local runtime state
└── tools/           # mcp.cljs — MCP bridge tool factory
```

The orchestration layer (`infra/agent/`, e.g. `hydration.cljs`) composes domain tool vectors. Domain namespaces should never import each other.

### Frontend Multi-Build System

Three concurrent builds run in dev:
1. TailwindCSS → `dist/app.css`
2. Vite bridges → `dist/bridge/*.es.js` (TypeScript APIs exposed to ClojureScript)
3. Shadow-cljs → `dist/cljs/*.js`

The shadow-cljs dev HTTP server on `:5173` (`frontend/shadow-cljs.edn` `:dev-http`) serves `dist/` and proxies `/api`, `/ws`, and `/health` to `http://127.0.0.1:8000`.

### Ingestion Driver Pipeline

Pluggable drivers handle data sources (local filesystem, audio, images, scrapers, GitHub, PromptDB, eta-mu and OpenCode session logs). Each driver implements the `kms-ingestion.drivers.protocol/Driver` protocol (`discover`, `extract`, `extract-batch`, `get-state`, `set-state`, `close`). A passive file system watcher (Java WatchService) triggers scans automatically; sync intervals are configurable per source.

### Contracts & Authorization

Declarative contract files under `contracts/` define actors, roles, and policies. The backend loads and evaluates these at runtime to gate requests. The development actor is `pi@open-hax.local` (system-admin role). API key resolution uses the `X-API-Key` header.

## Key Environment Variables

```bash
# Backend
KNOXX_API_KEY=<key>
KNOXX_API_KEY_USER_EMAIL=pi@open-hax.local   # optional override

# Ingestion
OPENPLANNER_BASE_URL=http://localhost:7777
OPENPLANNER_API_KEY=<key>
WORKSPACE_PATH=/home/err/devel
DATABASE_URL=postgresql://user:pass@host:5432/knoxx   # ingestion still uses PostgreSQL
```

The backend itself persists to MongoDB (`MONGODB_URI`, `MONGODB_DB`; see `backend/src/cljs/knoxx/backend/infra/mongo_client.cljs`).

## Hot Reload

Do **not** restart PM2/Knoxx processes unless the user explicitly asks. Shadow-cljs hot-reloads backend CLJS changes; Vite reloads frontend changes. If a restart seems necessary, report why and wait for approval.
