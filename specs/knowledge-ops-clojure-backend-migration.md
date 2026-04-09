# Knowledge Ops — Clojure Backend Migration

## Current Canonical Reading

- state: current migration spec with partially landed implementation
- canonical backend target today: `orgs/open-hax/knoxx/backend` (shadow-cljs + Fastify)
- canonical ingestion/query donor now lives at: `orgs/open-hax/knoxx/ingestion`
- `ragussy` references remain useful as donor context only

> *Keep the frontend. Replace the spine.*

---

## Purpose

Define the migration from the current Python/Ragussy-centered backend shape to a Clojure/OpenPlanner-centered backend shape.

The goal is not to throw away the useful frontend work. The goal is to stop depending on the weakest backend links.

---

## Current Reality

### Useful pieces from Ragussy

Backend pieces still worth keeping conceptually:
- `orgs/mojomast/ragussy/backend/app/api/proxx.py`
- `orgs/mojomast/ragussy/backend/app/services/embeddings_service.py`
- `orgs/mojomast/ragussy/backend/app/api/rag.py` (only as a reference for ingest/search behavior)

Frontend pieces worth reusing:
- `orgs/mojomast/ragussy/frontend/src/pages/CmsPage.tsx`
- `orgs/mojomast/ragussy/frontend/src/pages/IngestionPage.tsx`
- `orgs/mojomast/ragussy/frontend/src/App.tsx`

### Weak links

| Component | Why it is weak |
|-----------|----------------|
| Ragussy RAG backend | depends on unpublished or unreliable collaborator assumptions |
| Qdrant-centric ingest path | ties the system to one narrow indexing model |
| Python ingestion path | already being replaced by the Clojure service |
| Proxx embed path behind Ragussy | created current `502` failure mode for ingestion |

---

## Target Architecture

```
Frontend
  ├── CMS UI (reuse Ragussy page for now)
  ├── Ingestion UI (reuse Ragussy page for now)
  └── Widget / search surfaces

Clojure backend
  ├── kms-ingestion     (already exists, now writes to OpenPlanner)
  ├── kms-cms           (new, replaces Python cms.py)
  └── kms-query         (new, role/lake-aware query orchestration)

Infra services
  ├── OpenPlanner       (canonical ingest/search/event lake)
  ├── Proxx             (canonical generation + embeddings gateway)
  ├── Shibboleth        (SME review / labeling)
  └── PostgreSQL        (tenants, labels, document metadata if kept relational)
```

---

## Migration Principle

### Keep
- frontend surfaces
- schemas
- labels/export model
- tenant/document concepts

### Replace
- Ragussy as canonical ingestion/search backend
- Python ingestion backend
- Python CMS backend over time

### Canonicalize
- OpenPlanner for ingest + retrieval
- Proxx for generation
- Shibboleth for review

---

## Phase Plan

### Phase 1: Clojure ingestion becomes real

Done now:
- `kms-ingestion` service is live
- JSON body parsing fixed
- local driver now defaults to text-like files
- worker can classify files into role-scoped lakes
- worker can target OpenPlanner instead of Ragussy when configured

Files:
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj`
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/drivers/local.clj`
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`

### Phase 2: Introduce `kms-query` in Clojure

Responsibilities:
- role preset resolution (knowledge/dev/devsecops/data/c-suite)
- lake selection
- OpenPlanner search orchestration
- Proxx synthesis call

Endpoints:
- `POST /api/query/search`
- `POST /api/query/answer`
- `GET /api/query/presets`

### Phase 3: Replace Python CMS with `kms-cms`

Responsibilities:
- document CRUD
- visibility transitions
- publish/archive state machine
- AI draft generation through Proxx
- public/internal lake publication

This replaces:
- `packages/futuresight-kms/python/km_labels/routers/cms.py`

### Phase 4: Keep Python only where it still earns its keep

Candidates to keep temporarily:
- label CRUD/export if stable

Candidates to migrate later:
- all remaining tenant/document routes into Clojure

---

## OpenPlanner Contract

`kms-ingestion` writes file artifacts into OpenPlanner as typed events:

| OpenPlanner field | Value |
|-------------------|-------|
| `source` | `kms-ingestion` |
| `kind` | `docs` \| `code` \| `config` \| `data` |
| `source_ref.project` | `devel-docs` / `devel-code` / `devel-config` / `devel-data` |
| `source_ref.session` | ingestion source ID |
| `source_ref.message` | stable file ID |

This gives immediate role-scoped lakes without waiting for a new OpenPlanner document schema.

---

## Why This Is Better

| Old | New |
|-----|-----|
| Ragussy-centric | OpenPlanner-centric |
| Python ingestion | Clojure ingestion |
| one broad vector ingest path | lake-aware ingest with typed routing |
| collaborator-dependent | corpus-native infrastructure |
| hard to explain | "OpenPlanner stores, Proxx thinks, Shibboleth judges" |

---

## Existing Code References

| File | Role |
|------|------|
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj` | Live Clojure ingestion API |
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj` | File classification + OpenPlanner target switch |
| `services/openplanner/src/routes/v1/events.ts` | Canonical ingest endpoint |
| `services/openplanner/src/routes/v1/search.ts` | Canonical filtered search endpoint |
| `orgs/open-hax/proxx/specs/drafts/proxx-openplanner-integration.md` | Existing gateway integration plan |
| `orgs/mojomast/ragussy/frontend/src/pages/CmsPage.tsx` | Reusable CMS UI donor |
| `orgs/mojomast/ragussy/frontend/src/pages/IngestionPage.tsx` | Reusable ingestion UI donor |
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | Feature reference for future Clojure CMS |
| `packages/futuresight-kms/python/km_labels/routers/ingestion.py` | Feature reference for future Clojure ingest/query UI parity |

---

## Immediate Next Step

Build `kms-query` in Clojure before rewriting the frontend:
- one endpoint that queries OpenPlanner by role preset + lake selection
- one endpoint that sends retrieved context to Proxx

That gives a real devel self-management demo even before a full Clojure CMS exists.

---

## Status

Specified 2026-04-02. Grounded in live code and current migration work.
