# Knowledge Ops — Graph Memory Reconciliation Spec

Date: 2026-04-05
Status: epic wrapper / current-state reconciliation

## Purpose

Reconcile the intended GraphRAG / graph-memory architecture with the system that actually exists today across:

- `orgs/open-hax/knoxx/`
- `orgs/open-hax/openplanner/`
- `orgs/octave-commons/graph-weaver/`
- `services/knoxx/`
- `services/openplanner/`

This spec is the canonical current-state bridge between:

- older knowledge-ops architecture docs,
- newer session-lake / cross-lake graph docs,
- current source code,
- and the live local dev deploy.

When this spec conflicts with stale README language, current source code and verified runtime behavior win.

---

## Epic decomposition

This document is an **epic wrapper**, not a direct execution spec.
The implementation work is split into child specs capped at 5 story points:

Execution order and milestone gates live in:

- `knowledge-ops-graph-memory-roadmap.md`

### Now

- `knowledge-ops-knoxx-health-route-coherence.md` — 3
- `knowledge-ops-kms-openplanner-ingest-arity-fix.md` — 2
- `knowledge-ops-openplanner-graph-population-smoke.md` — 5
- `knowledge-ops-myrmex-openplanner-write-recovery.md` — 3
- `knowledge-ops-graph-weaver-live-sync-truth.md` — 5
- `knowledge-ops-graph-memory-runtime-smoke-e2e.md` — 3

### Next

- `knowledge-ops-knoxx-graph-query-contract-v1.md` — 3
- `knowledge-ops-docs-source-of-truth-normalization.md` — 2

### Later

- `knowledge-ops-openplanner-derived-edge-projections-slice.md` — 5
- `knowledge-ops-adaptive-expand-policy-seam.md` — 2
- `knowledge-ops-adaptive-expand-policy-telemetry.md` — 2

Do not execute this parent spec directly. Pull child specs instead.

---

## Executive Summary

The stack already contains most of the required GraphRAG substrate.
The missing piece is not a new abstract graph architecture. The missing piece is **runtime coherence** between the canonical lake, the graph workbench, and the active producers.

### What already exists

- **OpenPlanner** already exposes canonical graph API routes:
  - `/v1/graph/stats`
  - `/v1/graph/export`
  - `/v1/graph/query`
  - `/v1/graph/nodes`
  - `/v1/graph/edges`
- **Graph-Weaver** already exposes graph workbench surfaces:
  - `searchNodes`
  - `neighbors`
  - `edges`
  - `nodePreview`
  - user-layer graph mutations
- **Knoxx backend** already exposes graph- and memory-aware agent tools:
  - `semantic_query`
  - `memory_search`
  - `memory_session`
  - `graph_query`
- **Knoxx session graph projection** already exists in the backend import/runtime code.
- **KMS ingestion** already writes `graph.node` / `graph.edge` events into OpenPlanner.

### What is currently broken

- `knoxx-backend` is unhealthy in the live local stack.
- `kms-ingestion` is repeatedly failing on an OpenPlanner ingest arity bug.
- `myrmex` is paused behind OpenPlanner backpressure.
- live `OpenPlanner` graph export is empty.
- live `Graph-Weaver` is configured to read from `openplanner-graph`, but is visibly serving stale persisted graph state while reporting sync failure.

### Immediate conclusion

The next step is **not** new traversal cleverness.
The next step is:

1. restore producer → OpenPlanner writes,
2. ensure OpenPlanner graph export is populated,
3. ensure Graph-Weaver reflects current canonical OpenPlanner state,
4. then freeze the bounded agent-facing graph contract,
5. then add adaptive traversal later.

---

## Canonical Source / Runtime Split

## Source homes

These are the canonical implementation homes:

- `orgs/open-hax/knoxx/`
- `orgs/open-hax/openplanner/`
- `orgs/octave-commons/graph-weaver/`

## Runtime / devops homes

These are the local runtime wrappers and state homes:

- `services/knoxx/`
- `services/openplanner/`

## Contract

- `orgs/**` owns source code and versioned architecture.
- `services/**` owns compose wiring, local runtime state, and host-local secrets/overrides.
- Do not treat `services/**` as the source-of-truth implementation home.
- Do not treat `orgs/**` as the mutable runtime home.

This is already explicit in `services/openplanner/README.md` and should be treated as canonical for deployment placement.

---

## Verified Current State

## Knoxx

### Current implementation reality

The live backend is **not** the older Python/FastAPI backend described in stale docs.
The current runtime backend is the CLJS/Node backend in:

- `orgs/open-hax/knoxx/backend/package.json`
- `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`

The package name is:

- `@open-hax/knoxx-backend-cljs`

### Existing agent/runtime graph surfaces

Knoxx backend already defines:

- `semantic_query`
- `semantic_read`
- `memory_search`
- `memory_session`
- `graph_query`

and already implements OpenPlanner helpers:

- `openplanner-graph-query!`
- `openplanner-graph-export!`

Knoxx also already emits session graph events through:

- `session-graph-node-event`
- `session-graph-edge-event`
- `session-text-graph-events`

### Current local runtime status

Observed in `services/knoxx` local stack:

- `graph-weaver` healthy
- `kms-ingestion` healthy at container level
- `myrmex` running
- `nginx` healthy
- `knoxx-backend` running but unhealthy

Observed through nginx:

- `GET /health/knoxx` returns `503`
- response body reports `fetch failed`

Observed from backend logs:

- `/api/memory/sessions` repeatedly returns `502`
- `/health` repeatedly returns `503`
- proxx health/model endpoints are sometimes slow but generally recover

### Current product implication

Knoxx already has the right conceptual tool surface for graph memory, but the live backend/runtime path is not healthy enough to act as the stable integration anchor yet.

---

## OpenPlanner

### Current implementation reality

OpenPlanner already implements graph routes in source code:

- `src/routes/v1/graph.ts`

Current graph route family includes:

- `/v1/graph/stats`
- `/v1/graph/nodes`
- `/v1/graph/edges`
- `/v1/graph/export`
- `/v1/graph/query`

OpenPlanner also has specs explicitly covering:

- graph events
- graph export/query
- web edge salience and backbone projections
- MongoDB-only migration

### Current storage/runtime reality

Observed local runtime (`services/openplanner`):

- `openplanner` healthy
- `mongodb` healthy
- `mongot` running
- `chroma` also running

Observed `GET /v1/health`:

- storage backend: `mongodb`
- vector collections:
  - `event_chunks`
  - `compacted_vectors`

### Current graph data reality

Observed live endpoints:

- `GET /v1/graph/stats` → `nodeCount: 0`, `edgeCount: 0`
- `GET /v1/graph/export?projects=devel,web,knoxx-session` → empty nodes/edges
- `GET /v1/graph/query?...` → empty nodes/edges
- `GET /v1/sessions?limit=5` → empty rows
- `POST /v1/search/fts` on `project=devel` → empty rows

### Current product implication

OpenPlanner has the correct route surface, but the canonical graph/memory lake is currently empty in the live local runtime.
The problem is therefore upstream ingestion and/or runtime pathing, not absence of API design.

---

## Graph-Weaver

### Current implementation reality

Graph-Weaver is already the graph workbench service, not just a crawler.
Its documented and implemented surfaces include:

- GraphQL inspect/query
- node preview
- graph view export for rendering
- mutation surfaces for user-layer graph changes
- runtime config changes
- websocket invalidation

When `GRAPH_WEAVER_LOCAL_SOURCE=openplanner-graph`, Graph-Weaver rebuilds the local graph from:

- `GET /v1/graph/export`

via:

- `src/openplanner-graph.ts`

### Current local runtime reality

Observed `GET /api/status`:

- `nodes: 69027`
- `edges: 78439`
- `localSourceMode: openplanner-graph`
- `includeWebLayer: false`
- `webCrawlEnabled: false`
- `localSync.ok: false`
- `error: "fetch failed"`

Observed GraphQL search still returns graph nodes.

### Interpretation

Graph-Weaver is currently serving a large graph state despite OpenPlanner graph export being empty and local sync failing.
That strongly suggests the rendered graph is coming from stale persisted state rather than a fresh canonical OpenPlanner rebuild.

### Current product implication

Graph-Weaver must be treated as a workbench surface over canonical graph state, but **today it is not a trustworthy reflection of current OpenPlanner runtime state**.
That gap must be closed before any higher-level GraphRAG behavior is considered reliable.

---

## KMS Ingestion and Myrmex

## KMS ingestion

Source-level evidence:

- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/graph.clj`
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`

KMS ingestion already produces:

- `graph.node`
- `graph.edge`

and posts them into OpenPlanner.

### Live failure

Observed logs show repeated backpressure entries with the same root cause:

- `Wrong number of args (7) passed to: kms-ingestion.jobs.worker/ingest-via-openplanner!`

This means the local producer path into OpenPlanner is currently broken at runtime.

## Myrmex

The local `services/knoxx` stack includes:

- `myrmex`
- `shuvcrawl`
- `eros-eris-field-app`

The intended architecture is already clear:

- Myrmex owns crawl/orchestration
- Graph-Weaver is the graph workbench
- OpenPlanner is the canonical graph/event lake

### Live failure

Observed logs show repeated OpenPlanner backpressure and transport failure:

- health check failed: `fetch failed`
- writes paused
- large frontier remains pending

### Current product implication

Both canonical producers (`kms-ingestion` and `myrmex`) are currently unable to maintain the canonical OpenPlanner graph state reliably.
That is the central operational defect in the current GraphRAG memory stack.

---

## Canonical Decisions

## Decision 1 — OpenPlanner is the canonical graph/event lake

All canonical graph facts belong in OpenPlanner event storage.

Canonical graph record kinds are:

- `graph.node`
- `graph.edge`

OpenPlanner is the authoritative store for:

- node/edge events
- graph export/query routes
- session memory rows/events
- cross-lake graph projection

## Decision 2 — Graph-Weaver is the graph workbench, not a second canonical devel scanner

Graph-Weaver may maintain:

- local UI state
- layout state
- user-layer annotations/mutations
- workbench convenience views

But it should not be treated as a second canonical producer of `devel` graph truth.
For canonical `devel` facts, Graph-Weaver should consume OpenPlanner’s graph export.

## Decision 3 — Knoxx consumes graph memory through bounded tools, not raw store internals

Knoxx should continue to expose graph and memory to agents through bounded tools such as:

- `semantic_query`
- `memory_search`
- `memory_session`
- `graph_query`

Agents should not couple directly to Graph-Weaver GraphQL or storage layout.

## Decision 4 — Hybrid retrieval is the default

The canonical retrieval loop is:

1. seed with FTS/vector and/or graph node search
2. expand a bounded local graph neighborhood
3. preview/summarize into promptable context
4. write back successful memory relations and session projections

Vector retrieval and graph retrieval are complementary, not competing systems.

## Decision 5 — Adaptive traversal is deferred until canonical writes and reads are trustworthy

Daimoi / semantic gravity / ACO-style traversal is valid future architecture.
But it is a **phase-3 concern**, not the next implementation slice.

Before adaptive traversal, the system must first guarantee:

- producers can write canonical graph data
- OpenPlanner graph routes return real data
- Graph-Weaver reflects current canonical state
- Knoxx graph/memory tools behave reliably against that state

---

## Current Doc Drift to Correct

## Stale or misleading current documents

### `orgs/open-hax/knoxx/README.md`

This README still describes:

- backend as FastAPI/Python
- older service boundaries

That is no longer the live backend reality.

### `services/knoxx/README.md`

This README is closer to the live stack, but still carries legacy naming and mixed-era service framing.

### Knowledge-ops corpus

The `knowledge-ops-*` spec set contains both active contracts and legacy-donor material.
Current runtime/source truth must override donor-era framing where they conflict.

## Canonical override rule

When README/spec documents disagree, use this precedence order:

1. verified live runtime behavior
2. current source code
3. current wrapper/runtime README (`services/**`)
4. current source README (`orgs/**`)
5. older donor / exploratory specs

---

## Implementation Plan

## Phase 0 — Restore runtime coherence

This is the immediate next slice.

### 0.1 Fix Knoxx backend health

- resolve why Knoxx health checks are failing through nginx/backend
- make `/health` return healthy when OpenPlanner + required dependencies are reachable enough for normal operation
- stop repeated `/api/memory/sessions` `502`s

### 0.2 Fix KMS ingestion → OpenPlanner arity/runtime bug

- repair `ingest-via-openplanner!` call/definition mismatch
- verify both document and graph events can be posted successfully
- verify backpressure clears after successful writes

### 0.3 Fix Myrmex → OpenPlanner write path

- verify health target and network path to OpenPlanner
- restore write success
- confirm frontier resumes and pending writes drain

### 0.4 Reconcile Graph-Weaver with canonical OpenPlanner state

- prove whether current rendered graph comes from stale persisted state
- make `openplanner-graph` mode reflect current OpenPlanner export, not old overlay state
- if stale cache fallback is preserved, surface it explicitly in UI/status as degraded mode

## Phase 1 — Freeze the canonical contract

Once Phase 0 is green:

- declare OpenPlanner the canonical graph/event lake
- declare Graph-Weaver the workbench surface over canonical graph state
- declare Knoxx tool-facing graph contract canonical
- update stale README/spec language accordingly

## Phase 2 — Stabilize bounded GraphRAG query semantics

Keep the initial agent-facing contract small:

- search
- expand
- preview
- write

Map these onto the already-existing surfaces:

- OpenPlanner graph export/query
- Graph-Weaver inspect/preview
- Knoxx `graph_query` tool

Do not expose raw traversal internals to agents yet.

## Phase 3 — Add adaptive traversal and edge intelligence

Only after the canonical loop is trustworthy:

- add daimon flow / semantic gravity as edge weighting
- add adaptive expansion strategies behind the same contract
- keep algorithm choice hidden behind bounded retrieval APIs

---

## Verification Targets

Phase 0 is complete when all of the following are true in the live local deploy:

1. `services/knoxx` backend health is green.
2. `kms-ingestion` no longer logs the OpenPlanner arity failure.
3. `myrmex` no longer sits in permanent OpenPlanner backpressure pause.
4. `GET /v1/graph/stats` in OpenPlanner returns non-zero counts.
5. `GET /v1/graph/export?projects=devel,web,knoxx-session` returns canonical nodes/edges.
6. `Graph-Weaver /api/status` reports successful sync in `openplanner-graph` mode.
7. Graph-Weaver-rendered graph corresponds to current OpenPlanner export, not stale persisted state.
8. Knoxx `graph_query` and memory tools operate successfully against live canonical data.

---

## Non-Goals for This Slice

- building a new graph database engine
- adding ACO traversal immediately
- expanding the agent tool surface beyond what already exists
- redesigning Knoxx UI from scratch
- reworking the entire knowledge-ops corpus before runtime coherence is restored

---

## Definition of Done

This reconciliation effort is done when:

- current source/runtime truth is documented in one place,
- stale architecture narratives are explicitly superseded,
- the live local deploy reflects canonical producer → OpenPlanner → Graph-Weaver flow,
- and further GraphRAG / graph-memory work can build on stable interfaces instead of conflicting realities.
