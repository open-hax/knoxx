# Knowledge Ops — Architecture Audit & Migration Spec

## Current Canonical Reading

- state: mixed-era migration spec, partially superseded
- canonical product/runtime today: **Knoxx**
- live Knoxx source roots:
  - `orgs/open-hax/knoxx/backend`
  - `orgs/open-hax/knoxx/frontend`
  - `orgs/open-hax/knoxx/ingestion`
  - `services/knoxx`
- read references to `ragussy` and `futuresight-kms` here as historical donor or migration context unless a line explicitly calls out the current Knoxx implementation

> *Kill Ragussy. Promote OpenPlanner. Make services thin. Package the runtime.*

---

## The Problem

Services contain source code. That's wrong.

```
BEFORE (broken):

services/openplanner/    ← source code lives here
services/ragussy/        ← source code lives here
services/kms-ingestion/  ← source code lives here
services/futuresight-kms/ ← compose + nginx + vendored code

packages/openplanner-*   ← JS client wrapper
packages/futuresight-kms/ ← Python + TypeScript source code
orgs/octave-commons/cephalon/packages/cephalon-ts/     ← TypeScript source code
```

Everything is backwards. The runtime should be a **package**. The service should be a **compose config** that says "run this package with these env vars."

And more importantly:

This is **not** a client-demo architecture.

This is a system intended to be useful to the workspace owner first:
- managing `devel`
- querying code, docs, configs, and live events
- reviewing dependency structure
- operating truth/control-plane workbenches
- producing publishable content from the same corpus

Clients are a downstream use case of a system that must already be useful in-house.

---

## What We Have

### Working code (this session built or fixed)

| Component | Where | What | Status |
|-----------|-------|------|--------|
| CMS router | `packages/futuresight-kms/python/km_labels/routers/cms.py` (523 lines) | Document CRUD, visibility states, AI draft, publish/archive | Working |
| Ingestion router | `packages/futuresight-kms/python/km_labels/routers/ingestion.py` (644 lines) | Driver-based file discovery, job management, WebSocket/SSE | Working |
| Labels router | `packages/futuresight-kms/python/km_labels/routers/labels.py` (299 lines) | 8-dimension label CRUD, filters | Working |
| Export router | `packages/futuresight-kms/python/km_labels/routers/export.py` (315 lines) | SFT JSONL, RLHF preference, manifest | Working |
| Clojure ingestion | `orgs/open-hax/knoxx/ingestion/` | File discovery, role-scoped lake classification, OpenPlanner ingest | Working |
| Clojure query | `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj` | Federated FTS, role presets, grounded synthesis | Working |
| Leads enrichment | `orgs/mojomast/ragussy/backend/app/api/main.py` | RDAP + ISP + cloud provider | Working (built this session) |
| Leads UI | `orgs/mojomast/ragussy/frontend/src/pages/Leads.tsx` | Enrichment stats, host table | Working (built this session) |
| QueryPage | `orgs/mojomast/ragussy/frontend/src/pages/QueryPage.tsx` | Federated search + synthesis | Working (built this session) |
| Chat Lab fix | `orgs/mojomast/ragussy/backend/app/api/ragussy.py` | Health URL | Fixed this session |
| OpenPlanner | `services/openplanner/` | Event lake, search, embeddings, job queue, auth | Working |
| Cephalon TS | `orgs/octave-commons/cephalon/packages/cephalon-ts/` | Agent framework, memory, OpenPlanner client | Working |
| Shibboleth | `orgs/octave-commons/shibboleth/` | Adversarial labeling DSL, chat lab | Working |
| Proxx | `orgs/open-hax/proxx/` | LLM gateway | Working, external users |
| Our-gpus | `orgs/shuv/our-gpus/` + `services/our-gpus/` | Ollama discovery, leads | Working, 10K+ hosts |
| Portal | `services/portal/` | Dashboard with links | Working |

### Ragussy — what it is

| Component | What | Should keep? |
|-----------|------|:---:|
| `ragussy/backend/app/api/rag.py` | RAG ingest/search (Qdrant) | No — OpenPlanner replaces |
| `ragussy/backend/app/api/openai.py` | OpenAI-compat chat (llama.cpp) | No — Proxx replaces |
| `ragussy/backend/app/api/proxx.py` | Proxy to Proxx | No — talk to Proxx directly |
| `ragussy/backend/app/api/shibboleth.py` | Handoff to Shibboleth | Move to km_labels |
| `ragussy/backend/app/services/embeddings_service.py` | BGE-M3 embeddings | Reference only |
| `ragussy/frontend/src/pages/*` | React pages (ChatLab, CMS, Ingestion, Query) | Keep as UI components |
| `ragussy/frontend/src/lib/api.ts` | API client | Migrate to km_labels |
| `ragussy/backend/app/api/server.py` | llama.cpp lifecycle | Keep for local model serving only |

### What to kill

| Component | Why |
|-----------|-----|
| Ragussy as the central backend | It's a thin proxy over Qdrant + llama.cpp + Proxx. OpenPlanner replaces Qdrant. Proxx replaces llama.cpp. The proxy layer adds nothing. |
| `services/openplanner/` as source code | Should be a package. Service should be compose config. |
| `services/futuresight-kms/kms-ingestion/` as source code | Should be a package. Service should be compose config. |
| `packages/futuresight-kms/python/` as the primary backend | Split: document management goes to OpenPlanner, labeling stays, export stays |

---

## The Architecture You Want

```
PACKAGES (source code):

openplanner/               ← data lake runtime (package)
  src/
    routes/                ← all endpoints (events, docs, search, cms, ingest)
    lib/                   ← duckdb, chroma, embedding-models, job queue
    plugins/               ← auth, metrics, storage
  package.json             ← published as @openhax/openplanner

cephalon-ts/               ← agent framework (package)
  src/
    memory/                ← OpenPlanner client, memory management
    inference/             ← Proxx client, chat completions
    cognition/             ← reasoning, planning
  package.json             ← published as @promethean-os/cephalon

futuresight-kms/           ← schema + bridge clients (package)
  src/                     ← Zod schemas, bridge clients
  python/km_labels/        ← label CRUD + export (not CMS)
  frontend/                ← shared components (Labeler, Widget)
  package.json             ← published as @openhax/futuresight-kms

chat-ui/                   ← shared chat components (package)
  src/components/          ← ChatPanel, MessageBubble, Composer
  src/hooks/               ← useChat, useChatStream
  src/types/               ← ChatMessage, ChatConfig
  package.json             ← published as @workspace/chat-ui

our-gpus/                  ← exposure scanning (package)
  app/                     ← Shodan integration, probes, leads
  package.json             ← published as @openhax/our-gpus


SERVICES (compose configs):

services/openplanner/
  docker-compose.yml       ← references packages/openplanner
  .env                     ← env vars
  config/                  ← nginx, prometheus, grafana

services/knowledge-workbench/
  docker-compose.yml       ← references: openplanner, proxx, shibboleth, km_labels
  .env                     ← all env vars
  config/                  ← nginx routing, prometheus

services/cephalon-hive/
  docker-compose.yml       ← references: openplanner, proxx
  .env                     ← env vars
```

---

## What OpenPlanner Becomes

OpenPlanner stops being "the event lake" and becomes **the entire product runtime**:

### Current (events only)

- `POST /v1/events` — ingest events
- `POST /v1/search/fts` — full-text search
- `POST /v1/search/vector` — vector search
- `GET /v1/events/:id` — get event
- Jobs API — import/export
- Blob API — binary storage

### Extended (documents + CMS + gardens)

| New endpoint | Purpose |
|-------------|---------|
| `POST /v1/documents` | Ingest a document (chunk + embed + store) |
| `GET /v1/documents` | List documents with visibility filter |
| `GET /v1/documents/:id` | Get document |
| `PATCH /v1/documents/:id` | Update document content or visibility |
| `POST /v1/documents/:id/publish` | Set visibility=public |
| `POST /v1/documents/:id/archive` | Set visibility=archived |
| `POST /v1/query/search` | Federated search across lakes |
| `POST /v1/query/answer` | Search + synthesize through Proxx |
| `GET /v1/gardens` | List available workbench views |
| `GET /v1/gardens/:id` | Get one garden definition |

### Data model

Documents are events with `kind: "docs" | "code" | "config" | "data"`.
Visibility is metadata: `meta.visibility: "internal" | "review" | "public" | "archived"`.
Gardens are named projections and operator views over the same store.

One runtime. One canonical store. No separate CMS knowledge base.

### Gardens

Not every useful surface is a chat.

OpenPlanner should expose named workbench views called **gardens**. A garden is:
- a saved projection over one or more lakes
- a dedicated operator surface
- optionally visual, tabular, graph-based, or truth-oriented

Known devel gardens:

| Garden | Existing implementation | Purpose |
|--------|-------------------------|---------|
| `devel-deps-garden` | `services/devel-deps-garden/` | dependency graph review for the workspace |
| `truth-workbench` | `services/eta-mu-truth-workbench/` | truth-resolution and control-plane view |
| `query` | current `kms-query` surface | federated text retrieval and synthesis |
| `ingestion` | current `kms-ingestion` surface | file/system intake and routing |

The product is not just “CMS + chat.” It is a **gardened workbench system** over a shared runtime.

---

## What Cephalon Becomes

Cephalon becomes the AI engine. It talks to OpenPlanner for data and Proxx for inference.

```
Cephalon TS:
  ├── memory/           ← OpenPlanner client (already exists)
  ├── inference/        ← Proxx client (NEW)
  ├── cognition/        ← reasoning, planning (existing)
  └── tools/            ← file read, shell, MCP (existing)

Cephalon is the "brain" that:
  1. Searches OpenPlanner for context
  2. Sends context + question to Proxx
  3. Returns grounded answers
  4. Manages conversation memory in OpenPlanner
```

Cephalon already has the OpenPlanner client (`orgs/octave-commons/cephalon/packages/cephalon-ts/src/openplanner/client.ts`). It needs a Proxx client and a query engine that combines search + synthesis.

Cephalon is what powers:
- collection-aware chat
- grounded synthesis
- garden-specific operators
- multi-lake reasoning

---

## What Gets Killed

| Component | Replaced by |
|-----------|-------------|
| Ragussy `/api/rag/search` | OpenPlanner `/v1/search/*` |
| Ragussy `/api/rag/ingest/text` | OpenPlanner `/v1/documents` |
| Ragussy `/api/proxx/chat` | Cephalon → Proxx directly |
| Ragussy `/v1/chat/completions` | Proxx directly |
| Ragussy llama.cpp lifecycle | Local model serving only (optional) |
| km_labels `documents` table | OpenPlanner events with visibility metadata |
| km_labels CMS router | OpenPlanner document endpoints |
| `orgs/open-hax/knoxx/ingestion/` | OpenPlanner document ingestion |

---

## What Stays (km_labels, trimmed)

km_labels keeps:
- `tenants` table + router
- `km_labels` table + router (label CRUD)
- `export` router (SFT/RLHF)
- `health` router

km_labels loses:
- `cms` router (moves to OpenPlanner)
- `ingestion` router (moves to OpenPlanner)

km_labels becomes a thin labeling service that talks to OpenPlanner for document access and Proxx/Cephalon for inference.

---

## Roadmap

### Phase 1: OpenPlanner document endpoints (week 1-2)

- Add `documents` table to OpenPlanner DuckDB/MongoDB
- Add `POST /v1/documents` — ingest document
- Add `GET /v1/documents` — list with visibility filter
- Add `PATCH /v1/documents/:id` — update visibility
- Add `POST /v1/documents/:id/publish` — set public
- Add `POST /v1/documents/:id/archive` — set archived

### Phase 2: Cephalon query engine (week 2-3)

- Add Proxx client to cephalon-ts
- Add `search + synthesize` function
- Add federated search across lakes
- Wire to OpenPlanner search + Proxx chat

### Phase 3: Kill Ragussy, restructure services (week 3-4)

- Move OpenPlanner source to `packages/openplanner/`
- Create `services/openplanner/` compose config
- Move kms-ingestion source to `packages/kms-ingestion/`
- Create `services/knowledge-workbench/` compose config
- Remove Ragussy RAG/search/Qdrant dependency
- Migrate frontend pages to km_labels frontend
- Remove separate km_labels documents table

### Phase 4: Unified UI (week 4-5)

- File explorer ingestion (OpenPlanner document browse)
- Collection-scoped chat (Cephalon query + Proxx)
- Labeling UI (km_labels review queue)
- Synthesis canvas (Cephalon + Proxx deliverable)

### Phase 5: Devel Self-Management Verification (week 5)

- verify `devel-docs`, `devel-code`, `devel-config`, `devel-data`, `devel-events`
- verify dependency-review garden
- verify truth workbench integration
- verify chat/query over real workspace data
- verify label/export flow against real workspace questions
- verify synthesis over the live corpus

---

## Files to Create

| File | Purpose |
|------|---------|
| `packages/openplanner/src/routes/v1/documents.ts` | Document CRUD endpoints |
| `packages/openplanner/src/lib/documents.ts` | Document storage in DuckDB, chunking, warm-tier embedding |
| `orgs/octave-commons/cephalon/packages/cephalon-ts/src/inference/proxx-client.ts` | Proxx chat client |
| `orgs/octave-commons/cephalon/packages/cephalon-ts/src/inference/query-engine.ts` | Federated search + synthesis |
| `services/knowledge-workbench/docker-compose.yml` | Unified compose config |
| `services/knowledge-workbench/.env` | Environment template |

## Files to Kill

| File | Why |
|------|-----|
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | Replaced by OpenPlanner documents |
| `packages/futuresight-kms/python/km_labels/routers/ingestion.py` | Replaced by OpenPlanner document ingestion |
| `ragussy/backend/app/api/rag.py` | Replaced by OpenPlanner search |
| `ragussy/backend/app/api/proxx.py` | Replaced by Cephalon Proxx client |
| `services/futuresight-kms/kms-ingestion/` (Clojure CMS/ingestion) | Merged into OpenPlanner |

## Files to Migrate

| File | From | To |
|------|------|---|
| `ragussy/frontend/src/pages/ChatLabPage.tsx` | Ragussy | OpenPlanner frontend or km_labels |
| `ragussy/frontend/src/pages/CmsPage.tsx` | Ragussy | OpenPlanner frontend |
| `ragussy/frontend/src/pages/QueryPage.tsx` | Ragussy | OpenPlanner frontend |
| `ragussy/frontend/src/pages/IngestionPage.tsx` | Ragussy | OpenPlanner frontend |

---

## Status

Specified 2026-04-02. Architecture audit and migration plan.
