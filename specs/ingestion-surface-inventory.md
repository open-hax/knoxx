# Ingestion Surface Inventory

Last updated: 2026-04-11
Scope: `orgs/open-hax/openplanner`

This is the current write-surface inventory for anything that adds a document or event to the devel lake, plus the main places where ingestion state is tracked or split-brain can still occur.

## Canonical persistence sinks

### Document sink
- `src/routes/v1/documents.ts:278-310`
  - `POST /v1/documents`
  - canonical long-form document ingest surface
  - persists the event row and indexes vectors via `persistAndMaybeIndex`

### Event sink
- `src/routes/v1/events.ts:35-108`
  - `POST /v1/events`
  - canonical short-form event/graph/session ingest surface
  - writes directly with `upsertEvent(...)` and hot-tier vector indexing

### Shared document persistence helper
- `src/routes/v1/documents.ts:189-270`
  - `persistAndMaybeIndex(...)`
  - shared helper used by document and CMS routes
  - writes event row via `persistEvent(...)` at `src/routes/v1/documents.ts:201-225`
  - indexes vectors via `indexDocument(...)` at `src/routes/v1/documents.ts:231-270`

## OpenPlanner route-level document writers

### Direct document CRUD/publish/archive
- `src/routes/v1/documents.ts:278-310`
  - `POST /v1/documents`
- `src/routes/v1/documents.ts:359+`
  - `PATCH /v1/documents/:id`
- `src/routes/v1/documents.ts:290-314`
  - publish/archive codepaths also route through `persistAndMaybeIndex`

### CMS manual/doc workflow writers
- `src/routes/v1/cms.ts:141-172`
  - `POST /v1/cms/documents`
  - creates CMS docs in project `tenant_id`
- `src/routes/v1/cms.ts:175-198`
  - `PATCH /v1/cms/documents/:id`
- `src/routes/v1/cms.ts:201-219`
  - `DELETE /v1/cms/documents/:id` (soft-archive)
- `src/routes/v1/cms.ts:222-256`
  - `POST /v1/cms/draft`
  - AI-drafted docs still land via `persistAndMaybeIndex`
- `src/routes/v1/cms.ts:259-366`
  - `POST /v1/cms/publish/:id/:garden_id`
  - republishes doc state and queues translation jobs
- `src/routes/v1/cms.ts:370-402`
  - legacy `POST /v1/cms/publish/:id`
- `src/routes/v1/cms.ts:405-423`
  - `POST /v1/cms/archive/:id`
- `src/routes/v1/cms.ts:426-460`
  - `DELETE /v1/cms/publish/:id/:garden_id`
  - unpublish path writes a new doc state

## OpenPlanner route-level event writers

### Direct event ingest
- `src/routes/v1/events.ts:35-108`
  - `POST /v1/events`

### Background import job writing through event endpoint
- `src/routes/v1/jobs.ts:467-479`
  - ChatGPT import job sends imported conversation events into `/v1/events` via `app.inject(...)`

## External writers into OpenPlanner

### Knoxx backend document ingestion -> OpenPlanner documents
- `packages/knoxx/backend/src/cljs/knoxx/backend/document_state.cljs:240-406`
  - `start-document-ingestion!`
  - scans selected/full files from Knoxx docs root and batches them into OpenPlanner docs
- `packages/knoxx/backend/src/cljs/knoxx/backend/openplanner_memory.cljs:31-65`
  - `upsert-openplanner-document!`
  - actual `POST /v1/documents` call
- `packages/knoxx/backend/src/cljs/knoxx/backend/openplanner_memory.cljs:67+`
  - `batch-upsert-openplanner-documents!`

### Knoxx backend event/memory writers -> OpenPlanner events
- `packages/knoxx/backend/src/cljs/knoxx/backend/openplanner_memory.cljs:536-539`
  - posts run/session graph memory to `/v1/events`
- `packages/knoxx/backend/src/cljs/knoxx/backend/session_titles.cljs:229-240`
  - persists generated session titles to `/v1/events`

### Knoxx ingestion worker -> OpenPlanner documents + events
- `packages/knoxx/ingestion/src/kms_ingestion/jobs/ingest_support.clj:97-145`
  - `ingest-via-openplanner!`
  - writes document payloads to `/v1/documents`
  - writes graph/file relationship events to `/v1/events`
- `packages/knoxx/ingestion/src/kms_ingestion/jobs/ingest_support.clj:58-75`
  - helper `post-openplanner-events!`

## Source-state / ingestion-tracking writers

These do not write OpenPlanner docs/events directly, but they affect auditability and “what has been ingested” state.

- `packages/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj:28-40`
  - `persist-result!`
  - writes per-file ingestion status into `ingestion_file_state`
- `packages/knoxx/ingestion/src/kms_ingestion/db.clj:316-333`
  - `upsert-file-state!`
  - canonical source-side state table writer
- `packages/knoxx/backend/src/cljs/knoxx/backend/document_state.cljs:265-406`
  - Knoxx local progress/history/indexed-file tracking atom
  - this is UI/runtime state, not canonical lake storage

## Current inventory / audit surfaces

### Flexible document inventory
- `src/routes/v1/documents.ts:313-333`
  - `GET /v1/documents`
  - now supports uncapped listing when `limit` is omitted, returns real `total`
- `src/routes/v1/documents.ts:335-357`
  - `GET /v1/documents/stats`
  - returns counts by project/kind/visibility/source/domain
  - supports filtering by project, kind(s), source, visibility, domain, created_by, path prefix, and selected metadata keys

### CMS inventory
- `src/routes/v1/cms.ts:101-129`
  - `GET /v1/cms/documents`
  - defaults to `kind=docs`, but now accepts flexible filters routed into the document filter builder
- `src/routes/v1/cms.ts:490-516`
  - `GET /v1/cms/stats`
  - returns filtered total plus project-wide kind/source breakdown

### Ingestion source audit
- `packages/knoxx/ingestion/src/kms_ingestion/api/routes.clj:163-215`
  - `GET /api/ingestion/sources/:source_id/audit`
  - compares:
    - matching files from current source filters
    - source-state ingested/failed counts
    - OpenPlanner document count for `project=<tenant>` + `source=kms-ingestion` + `metadata.source_id=<source_id>`

## Remaining split-brain / consolidation risks

### Workspace browser now uses unified devel lake
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:46-65`
  - `inferKind()` now returns `docs/code/config/data` (not `devel-docs/devel-code/...`)
  - `inferFileTypesForKind()` maps from kind to file type extensions
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:81`
  - default kind state is `docs` (not `devel-docs`)
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:159-166`
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:182-189`
  - source creation now uses `collections: ['devel']` always
  - source name includes kind as a facet label: `folderName → devel (kind)`

### Default ingestion worker source is already unified
- `packages/knoxx/ingestion/src/kms_ingestion/server.clj:287-307`
  - default bootstrap source is `collections ["devel"]`
  - this is the desired single-lake direction

### CMS is still a content-oriented view, not the whole lake by default
- `src/routes/v1/cms.ts:109-113`
- `packages/knoxx/frontend/src/pages/CmsPage.tsx`
  - default view remains `kind=docs`
  - this is intentional, but the UI now allows switching to `code`, `config`, `data`, or `all`

## Decision implications

With the WorkspaceBrowserCard unified, the canonical lake model is now:

1. `project=devel` is the single canonical lake boundary
2. `kind` is a filter/facet (docs/code/config/data), not a separate lake name
3. all source creation goes through `collections: ['devel']`
4. graph-weaver runs in `openplanner-graph` mode for lake-backed graph views
5. `ingestion_file_state` is audit state only, not a second source of truth for corpus contents

## Verified clean state (2026-04-11)

- **Ingestion sources**: 1 source, `collections: ["devel"]`
- **MongoDB events**: 574,333 total events; documents by kind: code (7802), docs (4591), config (3817), data (67)
- **Old collection metadata**: 0 documents with `devel-docs/devel-code/devel-config/devel-data` in `metadata.collection`
- **Ingestion file state**: 89,423 records, all with `collections: ["devel"]`

No migration was needed — the corpus was already unified. The WorkspaceBrowserCard change prevents future split-lake source creation.

## Ingestion audit findings (2026-04-11)

Running `GET /api/ingestion/sources/:source_id/audit` revealed:

| Metric | Count |
|--------|-------|
| Matching files | 76,495 |
| State: ingested | 6,913 |
| State: failed | 71,270 |
| State: deleted | 11,268 |
| OpenPlanner documents | 6,801 |

**Analysis of "failed" files:**
- Files marked `failed` actually exist on disk — they are NOT orphaned
- The `metadata.error` field shows the real cause: "Read timed out", "Connection refused"
- These are legitimate ingestion failures from previous runs that should be retried
- The orphan detection bug was: unchanged files were being incorrectly flagged as orphaned because discovery only returns new/changed files

**Fix applied:**
- `worker.clj`: Changed orphan detection to check file existence directly via `java.io.File.exists()` instead of relying on discovery results
- This prevents unchanged files from being incorrectly marked as deleted

**Remaining work:**
- Retry the 71,270 failed files when OpenPlanner is stable
- Consider adding a bulk retry endpoint for failed files
