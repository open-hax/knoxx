# Knoxx Backend

Knoxx backend is the Node 22 + shadow-cljs + Fastify control plane for the
Knoxx agent workbench. It owns HTTP/WebSocket transport, auth/RBAC, contract
loading and policy materialization, agent runtime orchestration, MCP exposure,
OpenAI-compatible model proxying, and integrations with Proxx, OpenPlanner,
Redis, PostgreSQL, ingestion, voice, Discord, media, studio/audio, and workspace
tools.

## Current architecture

```text
backend/
├── package.json
├── shadow-cljs.edn
├── scripts/
│   ├── run-shadow-tests-ci.mjs
│   ├── run-shadow-tests-coverage-ci.mjs
│   └── start-server-dev.cljs
├── src/cljs/knoxx/backend/
│   ├── entrypoint.cljs      # shadow-cljs :server/:server-dev init-fn
│   ├── bootstrap.cljs       # startup, Fastify registration, lifecycle hooks
│   ├── domain/              # business domains: agent, contracts, MCP, Discord, media, voice, etc.
│   ├── infra/               # config, HTTP, DB, Redis, stores, route registration
│   ├── shape/               # route/tool/session data-shape helpers
│   ├── runtime/             # process-local runtime state
│   ├── law/                 # contract/law helpers
│   ├── extern/              # JS/eta-mu/Proxx interop adapters
│   └── tools/               # tool adapters and MCP helpers
├── test/cljs/               # cljs.test suites
├── dist-dev/                # server-dev watch output, not committed as source
└── dist/                    # production/verification output, not committed as source
```

The active runtime is all-CLJS after shadow compilation. There is no current
`src/server.mjs` bootstrap in the source tree. Node/npm modules are imported
from CLJS namespaces through shadow-cljs `:js-provider :import` and
`:keep-as-import` settings.

## Build targets

Defined in `shadow-cljs.edn`:

| Build | Purpose | Output |
|-------|---------|--------|
| `:server-dev` | Hot-reload development runtime with node ESM devtools | `dist-dev/server.js` |
| `:server` | Production/verification runtime | `dist/server.js` |
| `:test` | Interactive/autorun node test build | `target/test/test.cjs` |
| `:test-ci` | CI test build used by `scripts/run-shadow-tests-ci.mjs` | `target/test/test-ci.cjs` |
| `:app` | Legacy/export compatibility build; not the normal server runtime | `dist/app.js` |

Normal backend startup path:

```text
shadow-cljs :server or :server-dev
  -> knoxx.backend.entrypoint/init
  -> knoxx.backend.bootstrap/bootstrap!
  -> Fastify app + routes + background services
```

## Development workflow

From the repo root:

```bash
pnpm -C backend install
```

Run a backend watch build and dev server in two terminals:

```bash
# Terminal 1: compile and hot-reload backend CLJS
pnpm -C backend run watch

# Terminal 2: wait for the watch artifact, patch devtools defines if needed,
# then import dist-dev/server.js
pnpm -C backend run start:dev
```

Or use the single-terminal helper, which starts both processes and shuts them
down together:

```bash
backend/scripts/dev-watch.sh
```

If you explicitly need to import an already-built dev artifact without the
launcher, use:

```bash
pnpm -C backend run start:dev:direct
```

For the full local Knoxx stack, prefer the repo-root PM2 ecosystem:

```bash
# From the Knoxx root
pm2 start ecosystem.config.cjs

# Or from backend/
pm2 start ../ecosystem.config.cjs
```

Do not use PM2 file watching for backend source or `dist*` output. shadow-cljs
owns hot reload; PM2 should only own long-running process supervision.

## Production-style build

```bash
pnpm -C backend run build
pnpm -C backend run start
```

Equivalent direct commands from `backend/`:

```bash
pnpm exec shadow-cljs release server
node dist/server.js
```

## Configuration

Runtime config is read in `src/cljs/knoxx/backend/infra/config.cljs`. Important
variables:

```bash
HOST=0.0.0.0
PORT=8000
APP_NAME="Knoxx Backend CLJS"

WORKSPACE_ROOT=/path/to/workspace
KNOXX_EXTRA_WORKSPACE_ROOTS=/path/one:/path/two
KNOXX_MUSIC_LIBRARY_ROOT=/path/to/music
CONTRACTS_DIR=/path/to/knoxx/contracts

KNOXX_POLICY_DATABASE_URL=postgresql://user:pass@host:5432/knoxx
DATABASE_URL=postgresql://user:pass@host:5432/knoxx
REDIS_URL=redis://127.0.0.1:6379

PROXX_BASE_URL=http://127.0.0.1:8789
PROXX_AUTH_TOKEN=...
PROXX_DEFAULT_MODEL=glm-5
PROXX_EMBED_MODEL=nomic-embed-text:latest

OPENPLANNER_BASE_URL=http://127.0.0.1:7777
OPENPLANNER_API_KEY=...
KMS_INGESTION_URL=http://127.0.0.1:3003

KNOXX_BASE_URL=http://127.0.0.1:8000
KNOXX_PUBLIC_BASE_URL=http://localhost
KNOXX_API_KEY=...
KNOXX_API_KEY_USER_EMAIL=pi@open-hax.local

MCP_ENABLED=true
MCP_SERVERS=name:http://127.0.0.1:8010/mcp:http
OPENPLANNER_MCP_BASE_URL=http://openplanner-mcp:8010
SHOEDELUSSY_MCP_BASE_URL=

VOXX_URL=http://127.0.0.1:8787
VOICE_GATEWAY_API_KEY=dev-token
KNOXX_STT_BASE_URL=http://127.0.0.1:8010
```

Do not print or commit real env files. The root PM2 ecosystem loads host secrets
from `~/.knoxx/.env` by default.

## Route registration map

Startup route flow:

1. `bootstrap.cljs` creates the Fastify app and registers default plugins.
2. `infra.core/register-ws-routes!` registers `/ws/stream` realtime routes.
3. `infra.routes.auth/register-auth-routes` registers GitHub OAuth, signup,
   invite, cookie-session, and auth context routes.
4. `infra.core/register-app-routes!` calls `infra.routes.app/register-routes!`,
   which composes most `/api/*`, `/v1/*`, tool, memory, admin, model, document,
   studio, voice, translation, and workspace routes.
5. `infra.routes.tools.proxy/register-proxy-routes!` adds compatibility proxy
   surfaces for ingestion/OpenPlanner/session ingestion helpers.
6. `infra.routes.mcp/register-mcp-http-routes!` adds MCP OAuth and `/mcp`.

Representative backend surfaces:

| Area | Routes |
|------|--------|
| Health/config | `GET /health`, `GET /api/config`, `GET /api/data/health` |
| Auth | `GET /api/auth/config`, `GET /api/auth/login`, `GET /api/auth/context`, `POST /api/auth/logout`, signup/invite routes |
| Agent runtime | `POST /api/knoxx/chat`, `POST /api/knoxx/chat/start`, `POST /api/knoxx/direct`, `POST /api/knoxx/direct/start`, steer/follow-up/abort routes, run/session status routes |
| Realtime | `WS /ws/stream` |
| OpenAI compatibility | `GET /v1/models`, `POST /v1/chat/completions`, `POST /v1/embeddings` |
| Proxx | `GET /api/proxx/health`, `GET /api/proxx/models`, `POST /api/proxx/chat`, observability routes |
| Memory | `GET /api/memory/sessions`, `GET /api/memory/sessions/:sessionId`, `POST /api/memory/search` |
| Contracts | `/api/agent/contracts*`, `/api/admin/contracts*` |
| Admin/RBAC | `/api/admin/*` org, role, user, membership, actor mailbox, event-agent, Discord, trigger routes |
| Tools | `/api/tools/catalog`, read/write/edit/bash, websearch, email, Discord publish, MCP call routes |
| MCP facade | `/.well-known/oauth-*`, `/api/mcp/oauth/*`, `/api/mcp/tokens`, `GET/POST/DELETE /mcp` |
| Data/OpenPlanner | `/api/data/*`, `/api/openplanner/*`, document/database/graph routes |
| Ingestion proxy | `/api/ingestion/*`, `/api/ingestion-proxy/*` |
| Studio/audio | `/api/studio/*`, playlist, labels, audio asset, Discord media scan routes |
| Workspace media | `/api/workspace-media/raw`, `/api/workspace-media/audio-library`, ensure-dir/rename routes |
| Voice | STT/TTS HTTP routes plus `WS /ws/voice/tts` |
| Translation | `/api/translations/*` |

## Contracts, auth, and policy

Contracts are runtime inputs, not static documentation. Backend contract logic is
centered under `src/cljs/knoxx/backend/domain/contracts/` and route/admin glue
under `src/cljs/knoxx/backend/infra/routes/contracts.cljs`.

Policy data is materialized through PostgreSQL via
`src/cljs/knoxx/backend/infra/db/policy.cljs`. The backend synchronizes
contract-backed actors/users/memberships during startup. In development, an
`X-API-Key` matching `KNOXX_API_KEY` can resolve to
`KNOXX_API_KEY_USER_EMAIL` or `pi@open-hax.local` and then reapply that actor's
contract before serving the request.

## MCP HTTP facade

Knoxx exposes a Model Context Protocol server over HTTP:

- `POST /mcp` — JSON-RPC / Streamable HTTP requests.
- `GET /mcp` — SSE/session transport.
- `DELETE /mcp` — session close.

Auth flow:

1. MCP client starts at `GET /api/mcp/oauth/authorize?...`.
2. User logs in through Knoxx `/api/auth/login` if needed.
3. Knoxx presents consent/capability dials.
4. Client exchanges the PKCE code at `POST /api/mcp/oauth/token`.
5. Client calls `/mcp` with `Authorization: Bearer <access_token>`.

Discovery and management:

- `GET /.well-known/oauth-authorization-server`
- `GET /.well-known/oauth-protected-resource`
- `POST /api/mcp/oauth/register`
- `GET /api/mcp/tokens`
- `DELETE /api/mcp/tokens/:tokenId`

MCP access tokens are stored in Redis and intersected with the user's current
policy context; a token cannot grant capabilities above the user's membership.

## Verification

For backend code changes, run the backend shadow test build and inspect test
output for failures even if the compiler exits zero:

```bash
pnpm -C backend exec shadow-cljs compile test
```

Preferred package commands:

```bash
pnpm -C backend run lint
pnpm -C backend run typecheck
pnpm -C backend run test
pnpm -C backend run test:coverage
```

For production/runtime changes, also run:

```bash
pnpm -C backend exec shadow-cljs compile server
```

For docs-only changes, at minimum run a whitespace check from the repo root:

```bash
git diff --check backend/README.md
```

## Troubleshooting

### `dist-dev/server.js` does not exist

Run the watch build first:

```bash
pnpm -C backend run watch
```

Or build production output:

```bash
pnpm -C backend run build
```

### shadow-cljs watch is running but the dev server does not import

Use the dev launcher instead of importing `dist-dev/server.js` by hand:

```bash
pnpm -C backend exec nbb scripts/start-server-dev.cljs
```

The launcher waits for the shadow dev server and a runnable artifact, then
patches devtools defines only when needed.

### Node built-ins such as `fs` or `crypto` fail to resolve

Check the build's `:js-options` in `shadow-cljs.edn`. Runtime builds use
`:js-provider :import` and `:keep-as-import` for Node/npm modules that should
remain runtime imports.

### Route handler compiles but fails at runtime

Route helpers live in `knoxx.backend.shape.app-shapes`. Prefer small, explicit
route helper imports and keep route registration in domain-specific namespaces
under `infra/routes/`. If a route namespace grows too large, split by vertical
domain rather than creating generic utility dumps.

### `cljs$core$IFn$_invoke$arity$*` on a function ending in `!`

This has historically appeared in legacy/simple-optimized builds when `!`-suffixed
functions such as `route!` were passed through destructured maps and emitted as
namespace property references. If you see it:

1. Confirm which build target produced the JS.
2. Grep the compiled output for the generated symbol, for example
   `route_BANG_`.
3. Prefer direct namespace imports for core route helpers instead of passing them
   through large destructured helper maps in affected code.

### Legacy references to avoid

Some older helper scripts and docs may still mention `src/server.mjs`,
`src/policy-db.mjs`, `shadow-cljs release app`, local GGUF server control, or
`dist/app.js` as the main runtime. Those are legacy model-lab/export assumptions.
The normal backend service is `:server-dev`/`dist-dev/server.js` for development
and `:server`/`dist/server.js` for production-style verification.

## License

GPL-3.0-or-later, matching the package and repository license.
