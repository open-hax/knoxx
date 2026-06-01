---
uuid: "knoxx-futuresight-kms-cms-python-backend"
title: "Build Layer 2 CMS Python backend in km_labels"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---

# Build Layer 2 CMS Python backend in km_labels

> Parent epic: `knoxx-knowledge-ops-chat-widget-layers`
> Points: 5

## Purpose

Implement the server-side CMS boundary layer (Layer 2) inside the `km_labels` Python package, providing the CRUD, AI-draft, publish, and archive API endpoints that enforce the public/internal document visibility model.

## Scope

Create or extend the following files within `packages/futuresight-kms/python/km_labels/`:

- `models.py` — add the `Document` Pydantic/SQLModel with fields: `doc_id`, `tenant_id`, `title`, `content`, `visibility` (`"internal" | "review" | "public" | "archived"`), `source`, `source_path`, `domain`, `language`, `created_by`, `published_by`, `published_at`, `last_reviewed_at`, `ai_drafted`, `ai_model`, `ai_prompt_hash`, `metadata`
- `database.py` — add `documents` table migration; wire `Document` model to the existing DB connection
- `routers/cms.py` — implement all seven endpoints from the spec:
  - `GET /api/cms/documents` (list + filter by visibility/domain/source)
  - `POST /api/cms/documents` (create manual doc)
  - `PATCH /api/cms/documents/{id}` (update content, visibility, metadata)
  - `POST /api/cms/draft` (AI-generate draft via Ragussy, topic + tone + audience + sources)
  - `POST /api/cms/publish/{id}` (set visibility to `public`, triggers sync hook)
  - `POST /api/cms/archive/{id}` (pull doc back to `archived`)
  - `GET /api/cms/public` (returns all `public` docs — Layer 1 read endpoint)

## Definition of done

- All seven CMS endpoints return correct HTTP status codes and JSON shapes as specified in the epic
- `Document` visibility transitions are enforced: only `review` or `internal` docs can be published; only `public` docs can be archived
- `POST /api/cms/draft` calls Ragussy and stores the result as a `review`-visibility, `ai_drafted: true` document
- `GET /api/cms/public` returns only documents with `visibility = "public"` — query-level enforcement, not application-level filtering
- Existing `km_labels` routes (`labels`, `export`) continue to pass their test suite after the migration

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-widget-layers` on 2026-05-30.
