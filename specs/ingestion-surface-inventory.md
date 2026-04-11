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

### Workspace browser still proposes old split-lake names
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:45-64`
  - still infers `devel-docs`, `devel-code`, `devel-config`, `devel-data`
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:81-82`
  - default UI state still starts at `devel-docs`
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:159-166`
- `packages/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx:182-189`
  - source creation still uses that collection value

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

If the goal is exactly one representation of each devel file in one devel lake, the main remaining consolidation target is:

1. remove or rewrite `WorkspaceBrowserCard` old lake inference (`devel-docs` etc.)
2. keep `project=devel` as canonical lake boundary
3. treat `kind` as a filter/facet, not as a separate lake name
4. keep graph-weaver in `openplanner-graph` mode for lake-backed graph views
5. treat `ingestion_file_state` as audit state only, not a second source of truth for corpus contents
