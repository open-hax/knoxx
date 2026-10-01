# Knoxx

> **Roadmap:** [`ROADMAP.md`](ROADMAP.md) — this repo's slice. The hub, with the
> seam, ownership table and sequencing rule, is [eta-mu/ROADMAP.md](https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md).

Knoxx is a local-first knowledge operations and agent workbench. It combines a
shadow-cljs/Fastify backend, a shadow-cljs + React frontend, a JVM Clojure
ingestion worker, contract-backed policy/actor/model definitions, and adapters
for OpenPlanner, Proxx, MCP, voice, Discord, studio/audio, translation, and
workspace tooling.

The name comes from Fort Knox: a secure vault for a team's knowledge. The garden
motif comes from digital-garden publishing and curated knowledge spaces.

> Built with [GLM-5](https://z.ai) — part of the [z.ai](https://z.ai) startup ecosystem and the [Ussyverse](https://ussy.cloud).

## Current architecture

```text
┌────────────────────────────┐
│ Frontend                   │
│ shadow-cljs router + React │
│ Vite-built TS bridges      │
│ dev HTTP :5173             │
└──────────────┬─────────────┘
               │ /api, /ws, /health
               ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend                                                     │
│ Node 22 + shadow-cljs + Fastify on :8000                    │
│ auth/RBAC, contracts, agent runtime, MCP facade, API proxy  │
└──────┬─────────────┬─────────────┬──────────────┬──────────┘
       │             │             │              │
       ▼             ▼             ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────┐
│ Proxx    │   │ Ingestion│   │ MongoDB  │   │ OpenPlanner  │
│ models   │   │ Clojure  │   │ sessions │   │ memory/graph │
│ :8789    │   │ :3003    │   │ policy   │   │ :7777        │
└──────────┘   └────┬─────┘   └──────────┘   └──────────────┘
                    ▼
               PostgreSQL (ingestion job/source state)
```

## Repository layout

```text
knoxx/
├── backend/        # Node + shadow-cljs + Fastify backend
│   ├── src/cljs/knoxx/backend/
│   │   ├── domain/ # Agent, contracts, Discord, MCP, media, voice domains
│   │   ├── extern/ # The only home of raw JS interop (Fastify, fetch, Mongo, ...)
│   │   ├── infra/  # HTTP routes, config, Mongo stores, clients, lifecycle
│   │   ├── law/    # Contracts, validators, policy evaluation
│   │   ├── shape/  # Route/tool/session shape helpers
│   │   ├── tools/  # MCP tool exposure; per-domain tools are domain/*/tools.cljs
│   │   └── runtime/# Process-local runtime state
│   └── test/       # cljs/ (cljs.test), js/ (node --test), clj/, e2e/
├── frontend/       # shadow-cljs browser app + React/TS bridge components
│   ├── src/cljs/knoxx/frontend/ # CLJS routing and migrated pages
│   └── src/                  # React pages, components, API clients, tests
├── ingestion/      # JVM Clojure ingestion service and source drivers
├── contracts/      # EDN contracts for actors, roles, capabilities, models, etc.
├── kanban/         # Agent task board (markdown cards) — source of truth for work items
├── docs/           # Current design docs, audits, reports, and notes
├── cms/            # Folder-backed CMS drafts/content
├── shared/         # CLJS shared across backend and frontend (e.g. markup rendering)
├── Audio/, uploads/ # local creative/media assets
├── ecosystem.config.cjs # deprecated PM2 shim; host stack moved out (see below)
└── scripts/        # repo checks, local launchers, and verification scripts
```

The standalone `discord-bot/` package and the `Voice/`, `Graphics/`, and
`Symmetry/` workspaces were removed from the tree on 2026-05-21 (commit
`b3348eb1`). Discord integration now lives in the backend Discord gateway
(`knoxx.backend.extern.discord`, `backend/src/cljs/knoxx/backend/domain/discord/`).

## Runtime surfaces

### Backend

The backend is the primary control plane. It provides:

- Fastify HTTP and WebSocket transport.
- GitHub OAuth/cookie auth plus API-key dev actor fallback.
- MongoDB-backed policy store for users, orgs, memberships, credentials, and
  contract-backed actor sync (`infra/stores/mongo_policy_*.cljs`).
- MongoDB-backed session, run, and thread persistence
  (`infra/mongo_client.cljs`; Redis and PostgreSQL were removed from the backend
  in the E14 Mongo migration).
- Agent chat/direct routes powered by the Knoxx/eta-mu agent runtime.
- Contract catalog, validation, indexing, file watching, and admin editing.
- Proxx/OpenAI-compatible model proxy routes.
- MCP OAuth and Streamable HTTP facade.
- Workspace tools (`read`, `write`, `edit`, `bash`), websearch, email, Discord,
  OpenPlanner, semantic memory, studio/audio, voice/STT/TTS, multimodal, and
  translation routes.

Backend entrypoint:

- shadow build: `backend/shadow-cljs.edn`
- runtime init namespace: `knoxx.backend.entrypoint/init`
- bootstrap namespace: `knoxx.backend.bootstrap`
- default HTTP port: `8000`

### Frontend

The frontend is currently migration-mode hybrid:

- shadow-cljs owns the router and browser app entrypoint in
  `frontend/src/cljs/knoxx/frontend/app.cljs`.
- Vite builds TypeScript React bridge bundles consumed by CLJS.
- The shadow dev server serves `frontend/dist` and proxies `/api`, `/ws`, and
  `/health` to the backend.
- Primary routes include Chat, Mail, Studio, CMS, Contracts, Data, Gardens,
  Translations, Events, Agents, and `/ops/*` admin surfaces.
- Default dev HTTP port in `frontend/shadow-cljs.edn`: `5173`.

### Ingestion

The ingestion worker is a JVM Clojure service under `ingestion/`.

It handles:

- source registration and job control;
- workspace/file browsing and preview;
- local, audio, image, scraper, eta-mu session, OpenCode session, and PromptDB
  style drivers;
- passive watch and scheduled sync;
- OpenPlanner/Ragussy sink routing;
- semantic edge and translation worker hooks;
- contract-resolved ingestion defaults.

Default HTTP port: `3003`.

## Quick start: local host development

Prerequisites:

- Node.js 24+ for repository development and verification tooling, plus pnpm.
  The backend alone supports Node.js 22.19+: its locked ATProto dependencies
  require Node 22+, Discord voice requires 22.12+, and Undici requires 22.19+.
  Root browser verification uses `agent-browser`, which requires Node 24+.
- Java 21+ for shadow-cljs / Closure Compiler.
- Clojure CLI for `ingestion/` and for every backend shadow-cljs command.
  `backend/shadow-cljs.edn` runs in `:deps` mode, so shadow-cljs resolves its
  classpath through tools.deps and launches via `clojure`.
- MongoDB (replica set) for backend sessions/runs/policy; `MONGODB_URI`
  defaults to `mongodb://localhost:27017`, database `openplanner`.
  `compose.environment.yaml` provides one.
- PostgreSQL for the ingestion worker's source/job state (`DATABASE_URL`).
- Proxx for model access, usually on `http://127.0.0.1:8789`.
- OpenPlanner for durable memory/events/graph, usually on
  `http://127.0.0.1:7777`.

For the local Foresight/OpenPlanner/Proxx/Ollama stack, Knoxx can discover the
already-running application credentials without writing them into this repo:

```bash
pnpm local:check
pnpm local:start
```

See [Local resources, MongoDB, Proxx, and Ollama](docs/verification/local-resources.md)
for the connection contract and overrides.

Install dependencies from the Knoxx root:

```bash
pnpm install
```

Start backend in two terminals:

```bash
# Terminal 1: compile backend CLJS in watch mode
pnpm -C backend run watch

# Terminal 2: run the dev server output
pnpm -C backend run start:dev
```

Start ingestion:

```bash
cd ingestion
clojure -M:run
```

Start frontend:

```bash
pnpm -C frontend run dev
```

Open the frontend at:

```text
http://127.0.0.1:5173
```

The shadow dev HTTP proxy target is `http://127.0.0.1:8000`, hard-coded as
`:proxy-url` in `frontend/shadow-cljs.edn`; there is no environment override
for it, so edit that file when the backend runs elsewhere.
`VITE_KNOXX_BACKEND_URL` (default `http://knoxx-backend:8000`) only affects
ad-hoc `vite` dev/preview via `frontend/vite.config.ts`.

## Quick start: PM2 host stack

> **Status (2026-09-30):** the root `ecosystem.config.cjs` is now a deprecated
> shim. It throws unless `KNOXX_HOST_ECOSYSTEM_CONFIG` names an absolute path to
> a host ecosystem file, and then simply `require`s that file. Its header points
> at `services/openplanner/ecosystem.host.config.cjs`, which is not present in
> the current `open-hax/services` checkout. `backend/ecosystem.config.cjs` is a
> separate single-app file (`knoxx`: `npx shadow-cljs watch app`).

The host stack is expected to run the same processes as before:

- `knoxx-shadow` — `shadow-cljs watch server-dev` for backend CLJS.
- `knoxx-backend` — `nbb scripts/start-server-dev.cljs`, waiting for the watch
  build and then running `dist-dev/server.js`.
- `knoxx-frontend` — `pnpm dev` in `frontend/`, with Vite bridge watch builds
  plus shadow dev HTTP on `5173`.
- `knoxx-ingestion` — `clojure -M:run` on `3003`.

Keep host secrets out of this repository. Do not commit real credentials.

```bash
KNOXX_HOST_ECOSYSTEM_CONFIG=/absolute/path/to/ecosystem.host.config.cjs \
  pm2 start ecosystem.config.cjs
pm2 logs knoxx-backend
```

## Production-style build commands

```bash
# Backend
pnpm -C backend run build
pnpm -C backend run start

# Frontend
pnpm -C frontend run build

# Both, from the Knoxx root
pnpm run build
```

## Key environment variables

Backend (`backend/src/cljs/knoxx/backend/infra/config.cljs`):

```bash
HOST=0.0.0.0
PORT=8000
WORKSPACE_ROOT=/path/to/workspace
KNOXX_EXTRA_WORKSPACE_ROOTS=/path/one:/path/two
CONTRACTS_DIR=/path/to/knoxx/contracts

MONGODB_URI=mongodb://127.0.0.1:27017/?replicaSet=rs0   # infra/mongo_client.cljs
MONGODB_DB=openplanner

PROXX_BASE_URL=http://127.0.0.1:8789
PROXX_AUTH_TOKEN=...
PROXX_DEFAULT_MODEL=glm-5
OLLAMA_BASE_URL=http://127.0.0.1:11434
OLLAMA_DEFAULT_MODEL=gemma4:e2b
KNOXX_AGENT_MODEL_OVERRIDES=publication_translator=gemma4:e2b,publication_post_drafter=gemma4:e2b
KNOXX_AGENT_THINKING_OVERRIDES=publication_translator=off,publication_post_drafter=off
KNOXX_TRANSLATION_RUNNER=agent
PROXX_EMBED_MODEL=qwen3-embedding:8b
# read by the in-process OpenPlanner SDK (extern/openplanner_sdk.cljs)
EMBED_PROVIDER_BASE_URL=http://127.0.0.1:11434
EMBED_PROVIDER_MODEL=qwen3-embedding:8b
EMBED_PROVIDER_DIMENSIONS=1024
OPENPLANNER_BASE_URL=http://127.0.0.1:7777
OPENPLANNER_API_KEY=...
KMS_INGESTION_URL=http://127.0.0.1:3003

KNOXX_BASE_URL=http://127.0.0.1:8000
KNOXX_API_KEY=...
KNOXX_API_KEY_USER_EMAIL=pi@open-hax.local

MCP_ENABLED=true
MCP_SERVERS=name:http://127.0.0.1:8010/mcp:http
VOXX_URL=http://127.0.0.1:8787
KNOXX_STT_BASE_URL=http://127.0.0.1:8010
```

Ingestion (`ingestion/src/kms_ingestion/config.clj`):

```bash
PORT=3003
DATABASE_URL=postgresql://user:pass@host:5432/knoxx
REDIS_URL=redis://127.0.0.1:6379
WORKSPACE_PATH=/path/to/workspace
OPENPLANNER_BASE_URL=http://127.0.0.1:7777
OPENPLANNER_API_KEY=...
KNOXX_BACKEND_URL=http://127.0.0.1:8000
KNOXX_API_KEY=...
PROXX_BASE_URL=http://127.0.0.1:8789
PROXX_AUTH_TOKEN=...
INGEST_SCHEDULE_MODE=hybrid
PASSIVE_WATCH_ENABLED=true
INGEST_SINK_TYPE=openplanner
```

Frontend (ad-hoc Vite only; shadow dev HTTP ignores it):

```bash
VITE_KNOXX_BACKEND_URL=http://127.0.0.1:8000
```

## API surface snapshot

Representative backend routes:

| Area | Routes |
|------|--------|
| Health/config | `GET /health`, `GET /api/config`, `GET /api/data/health` |
| Auth/context | `GET /api/auth/config`, `GET /api/auth/login`, `GET /api/auth/context`, `POST /api/auth/logout`, invite/signup routes |
| Agent runtime | `POST /api/knoxx/chat`, `POST /api/knoxx/chat/start`, `POST /api/knoxx/direct`, `POST /api/knoxx/direct/start`, `POST /api/knoxx/steer`, `POST /api/knoxx/follow-up`, `POST /api/knoxx/abort`, `GET /api/knoxx/run/:runId/events`, `GET /api/knoxx/runs/:runId`, `WS /ws/stream` |
| OpenAI compatibility | `GET /v1/models`, `POST /v1/chat/completions`, `POST /v1/embeddings` |
| Proxx | `GET /api/proxx/health`, `GET /api/proxx/models`, `POST /api/proxx/chat`, observability routes under `/api/proxx/observability/*` |
| Memory | `GET /api/memory/sessions`, `GET /api/memory/sessions/:sessionId`, `POST /api/memory/search`, session title import/backfill routes |
| Contracts | `GET /api/agent/contracts`, `POST /api/agent/contracts/validate`, `GET/PUT /api/agent/contracts/:contractId`, admin contract routes under `/api/admin/contracts` |
| Admin/RBAC | org, role, user, membership, data-lake, actor mailbox, event-agent, Discord config, and trigger routes under `/api/admin/*` |
| Tools | `GET /api/tools/catalog`, `POST /api/tools/read`, `POST /api/tools/write`, `POST /api/tools/edit`, `POST /api/tools/bash`, websearch/email/Discord publish routes |
| MCP | `GET /.well-known/oauth-authorization-server`, `GET /.well-known/oauth-protected-resource`, `POST /api/mcp/oauth/register`, OAuth authorize/token routes, `GET/POST/DELETE /mcp` |
| Data/OpenPlanner | `/api/data/*`, `/api/openplanner/*`, document ingestion/status/history routes, graph export, database settings |
| Ingestion proxy | `/api/ingestion/*`, `/api/ingestion-proxy/*` |
| Studio/audio | `/api/studio/*`, labels, playlists, audio assets, Discord media scan routes |
| Workspace media | `/api/workspace-media/raw`, `/api/workspace-media/audio-library`, rename/ensure-dir routes |
| Voice | `GET /api/voice/stt/health`, `POST /api/voice/stt`, `GET /api/voice/tts/health`, `POST /api/voice/tts`, `WS /ws/voice/tts` |
| Translation | routes under `/api/translations/*` |

Representative ingestion routes:

| Route | Purpose |
|-------|---------|
| `GET /health` | Ingestion service health |
| `GET /api/ingestion/browse` | Browse workspace/source files |
| `GET /api/ingestion/file` | Preview a file |
| `PUT /api/ingestion/file` | Update a file through ingestion surface |
| `GET/POST /api/ingestion/sources` | Source list/create |
| `GET/POST /api/ingestion/jobs` | Job list/create/start |
| `POST /api/query/search` | Federated/source search |
| `POST /api/query/answer` | Grounded answer synthesis |
| `GET /api/query/gardens` | Garden listing |

## Contracts and policy

Contracts live under `contracts/` and are loaded by the backend as runtime data.
Current contract directories include `actors/`, `agents/`, `roles/`,
`capabilities/`, `policies/`, `model_families/`, `models/`, `runtime_features/`,
`source_modes/`, `sub_agents/`, `namespaces/`, `mcp_servers/`, `authentication/`,
`knoxx-session/`, and `fork-tales/`, plus `_defaults.edn` and the CMS
block/template registries. Retired or parked contracts live under
`contracts/.disabled/` and the top-level `disabled-contracts/`.

The Pi development actor is defined at:

```text
contracts/actors/pi.edn
```

In development, requests with `X-API-Key: $KNOXX_API_KEY` can resolve to
`KNOXX_API_KEY_USER_EMAIL` or `pi@open-hax.local`, then reapply the actor
contract before serving the request.

Example:

```bash
curl http://localhost:8000/api/auth/context \
  -H "X-API-Key: $KNOXX_API_KEY"
```

## Important docs

Start here when changing behavior:

- `docs/shadow-cljs-backend-rewrite.md` — CLJS backend migration and route parity (historical; migration complete).
- `backend/README.md` — current backend build targets, startup path, and route map.
- `docs/agent-runtime-workbench.md` — chat/workbench runtime doctrine and landed shape.
- `docs/contract-oriented-backend-refactor.md` — contract-domain architecture (historical proposal; see `docs/design/resource-architecture.md` for the implemented model).
- `docs/ingestion-contract-surfaces.md` — ingestion contract model and defaults.
- `docs/auth-and-onboarding.md` — auth and onboarding flow.
- `docs/admin-multitenancy-rbac.md` — org/user/role admin model.
- `docs/actor-realtime-socket-io-spec.md` — realtime/actor bus target shape (draft; not implemented).
- `kanban/` — agent task board (markdown cards); the source of truth for work items. (The former `specs/` tree was retired 2026-05-28; its history lives in git and the fork-tax tags.)

## Testing and verification

Backend:

```bash
pnpm -C backend run lint
pnpm -C backend run typecheck
pnpm -C backend run test
pnpm -C backend run test:coverage
```

Frontend:

```bash
pnpm -C frontend run typecheck
pnpm -C frontend run test
pnpm -C frontend run test:e2e
```

Ingestion:

```bash
cd ingestion
clj-kondo --lint src test
clojure -M:test
```

Repo-wide checks:

```bash
node scripts/lint-file-sizes.mjs
bash scripts/pre-push-checks.sh
pnpm run scan:duplication
```

## File size budgets

Knoxx includes a repo-local size linter for Clojure, ClojureScript, TypeScript,
and TSX files.

- warning threshold: 350 lines
- error threshold: 500 lines

Run the full check from the Knoxx root:

```bash
node scripts/lint-file-sizes.mjs
```

Or run package-local checks:

```bash
pnpm -C backend run lint:size
pnpm -C frontend run lint:size
```

## Git hooks

Knoxx ships a tracked pre-push hook that runs lint and typecheck gates before a
push:

- repo-wide size lint
- backend `clj-kondo`
- backend `shadow-cljs compile server`
- ingestion `clj-kondo`
- frontend size lint + TypeScript typecheck
- frontend changed-surface CLJS lint + migration manifest check
- discord-bot size lint + TypeScript typecheck — still listed in
  `scripts/pre-push-checks.sh`, but `discord-bot/` no longer exists, so these two
  checks currently fail (2026-09-30)

Install the tracked hook path once per clone:

```bash
bash scripts/install-hooks.sh
```

Run the same checks manually:

```bash
bash scripts/pre-push-checks.sh
```

Temporary escape hatch:

```bash
git push --no-verify
# or
KNOXX_SKIP_PRE_PUSH=1 git push
```

## License

GPL-3.0-or-later; see `LICENSE`.

## Deployment ownership

Knoxx owns application validation and portable packaging. Production image
builds, host placement, deployment, and live verification are owned by the
DigitalOcean stack in `open-hax/services`. A reviewed Services pull request
carrying `deploy` at merge time authorizes that stack; Knoxx pull-request labels
do not deploy a shared staging slot.
