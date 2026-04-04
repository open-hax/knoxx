# Knoxx shadow-cljs backend rewrite

## Goal

Replace the Python/FastAPI backend in `orgs/open-hax/knoxx/backend/` with a shadow-cljs + Fastify service, and move conversation/agent behavior onto the pi SDK so Knoxx can hold multi-turn context instead of re-answering statelessly per request.

## Non-goals

- Do **not** cut over Docker Compose until the new backend can cover the routes the current frontend actually depends on.
- Do **not** preserve llama.cpp orchestration just because the Python service has it today; prefer Proxx + pi-sdk as the new control plane unless a concrete UI flow still needs local model-lab semantics.

## Source-of-truth constraints

- HTTP server: Fastify
- Backend language/runtime: ClojureScript compiled by shadow-cljs
- Agent/session logic: `@mariozechner/pi-coding-agent`
- Theme/UI contracts remain unchanged in the frontend
- Current ingress topology through `services/knoxx/config/conf.d/default.conf` must keep working during migration

## Why this rewrite

The current main Knoxx query path is stateless:

- `frontend/src/pages/ChatPage.tsx` sends only the current `q`
- `ingestion/src/kms_ingestion/api/routes.clj` builds a one-shot answer prompt per request

The newer Knoxx-next UI expects a real `conversationId`, but the Python backend is still largely proxy/orchestration glue. A shadow-cljs backend with pi SDK sessions can own:

1. durable multi-turn conversation state
2. agentic tool use against the workspace
3. Proxx-backed model execution
4. a narrower, easier-to-evolve Fastify API surface

## Required compatibility surface

### Phase 1: bootstrap-compatible

These routes should exist first so the backend can stand up without Python:

- `GET /health`
- `GET /api/config`
- `GET /api/proxx/health`
- `GET /api/proxx/models`
- `POST /api/proxx/chat`
- `GET /api/knoxx/health`
- `POST /api/knoxx/chat`
- `POST /api/knoxx/direct`
- `ALL /api/knoxx/proxy/*`

### Phase 2: frontend parity

Port or replace the currently frontend-visible legacy routes:

- `GET /api/models`
- `POST /api/chat`
- `GET /api/runs`
- `GET /api/runs/:id`
- `GET/POST /api/lounge/messages`
- `GET /api/tools/catalog`
- `POST /api/tools/read`
- `POST /api/tools/write`
- `POST /api/tools/edit`
- `POST /api/tools/bash`
- `POST /api/shibboleth/handoff`
- `WS /ws/stream`
- `POST /v1/chat/completions`
- `POST /v1/embeddings`
- `GET /v1/models`

## Proposed architecture

## Runtime layers

1. **Fastify transport layer**
   - request parsing
   - auth/header rewriting for proxied Knoxx/Proxx calls
   - WebSocket fanout later if needed

2. **pi-sdk agent layer**
   - one `AgentSession` per Knoxx `conversationId`
   - in-memory first, persistent session files second
   - Proxx registered as the model provider
   - workspace tools scoped to `WORKSPACE_ROOT`

3. **Proxy/adaptor layer**
   - temporary passthroughs for old Knoxx endpoints
   - Proxx health/models/chat passthrough
   - eventual removal once direct pi-sdk agent routes replace them

## Model/provider plan

Use pi-sdk custom model configuration to register a `proxx` provider aimed at `PROXX_BASE_URL`, authenticated by `PROXX_AUTH_TOKEN`, defaulting to `PROXX_DEFAULT_MODEL` (currently `glm-5`).

That gives the agent layer a real model object without depending on the old Python orchestration layer.

## Conversation-memory contract

For agent-backed chat routes:

- if no `conversationId` is provided, mint one
- reuse the same pi `AgentSession` for subsequent turns with that `conversationId`
- return the same `conversationId` on every response
- add a regression test using a turn-1 nonce recall on turn 10

## Migration phases

### Phase 1

- add shadow-cljs backend package scaffold
- stand up Fastify server
- port health/config/proxx/knoxx-proxy routes
- add pi-sdk session service prototype

### Phase 2

- switch `api/knoxx/chat` and `api/knoxx/direct` to pi-sdk-backed sessions
- verify multi-turn recall with live probes
- wire Proxx default model through pi-sdk provider config

### Phase 3

- port tools and shibboleth routes
- decide whether legacy runs/models pages remain or are replaced
- port websocket/event streaming if still needed

### Phase 4

- replace backend Dockerfile/service to run the shadow-cljs build
- cut nginx/compose over to the new runtime
- delete Python code once route parity + tests are green

## Verification checklist

- shadow-cljs build succeeds
- Fastify service boots locally
- `/api/proxx/health` reports `glm-5`
- `/api/knoxx/chat` preserves memory across at least 10 turns
- live request log shows `glm-5` routed via Rotussy
- old Python container no longer required for active UI flows

## Current status snapshot

### Landed in the CLJS backend prototype

- Fastify bootstrap via `src/server.mjs`
- pi-sdk-backed conversation sessions with reusable `conversationId`
- `GET /health`
- `GET /api/config`
- `GET /api/settings`
- `PUT /api/settings`
- `GET /api/settings/knoxx-status`
- `GET /api/retrieval/stats`
- `GET /api/proxx/health`
- `GET /api/proxx/models`
- `POST /api/proxx/chat`
- `GET /api/models` (currently mapped from Proxx catalog)
- `GET /api/runs`
- `GET /api/runs/:runId`
- `GET/POST /api/lounge/messages`
- `GET /api/tools/catalog`
- `POST /api/tools/read`
- `POST /api/tools/write`
- `POST /api/tools/edit`
- `POST /api/tools/bash`
- `POST /api/knoxx/chat`
- `POST /api/knoxx/direct`
- `POST /api/shibboleth/handoff`
- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /v1/embeddings`
- bridge passthrough routes at `/api/knoxx/proxy/*`

### Still missing before Python can be deleted cleanly

- decide whether to keep or remove the remaining generic `/api/knoxx/proxy/*` compatibility bridge; the current frontend no longer calls it directly, but it still exists as a fallback surface
- broader event-stream parity beyond the current websocket surface if additional legacy channels are still required
- optional cleanup of obsolete Python-specific files after confidence is high enough

## Deployment status snapshot

- `services/knoxx` now builds `knoxx-backend` from the Node/shadow-cljs Dockerfile instead of the old Python runtime
- compose healthcheck for `knoxx-backend` now uses Node `fetch`
- live `docker compose up -d knoxx-backend nginx` succeeded
- `GET /health/knoxx` through nginx returned `{"status":"ok","service":"knoxx-backend-cljs"}`
- live `POST /api/knoxx/chat` through nginx preserved multi-turn memory after the backend cutover
- live `ws://localhost/ws/stream?session_id=...` upgraded successfully through nginx and delivered both `stats` and `lounge` packets
- websocket registration now lives in the JS ESM bootstrap, which wraps the CLJS route handler inside a Fastify plugin scope that correctly preserves websocket upgrade behavior
- the current frontend no longer has direct callers of `/api/knoxx/proxy/*`; `src/lib/nextApi.ts` now points document/database/retrieval-debug flows at direct `/api/...` routes
- the CLJS/pi-sdk agent runtime now includes custom semantic query tools:
  - `semantic_query` — search the active Knoxx corpus for relevant documents/snippets
  - `semantic_read` — read a corpus document by relative path after a semantic hit is identified
- the default Knoxx agent system prompt now explicitly treats passive semantic hydration as incomplete and tells the agent to use semantic query tools when grounding matters
- direct document-management routes are now CLJS-owned for:
  - `GET /api/documents`
  - `POST /api/documents/upload`
  - `DELETE /api/documents/*`
  - `GET /api/documents/content/*`
  - `POST /api/documents/ingest`
  - `POST /api/documents/ingest/restart`
  - `GET /api/documents/ingestion-status`
  - `GET /api/documents/ingestion-progress`
  - `GET /api/documents/ingestion-history`
  - `GET|POST|PATCH|DELETE /api/settings/databases*`
  - `POST /api/chat/retrieval-debug`
- live nginx validation succeeded for direct document upload + auto-ingest + document listing + ingestion progress/history
- live agent validation succeeded against a unique nonce in `semantic-agent-proof.md`, with the CLJS agent runtime returning the correct relative path and nonce from the active corpus
