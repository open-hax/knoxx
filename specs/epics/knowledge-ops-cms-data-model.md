# Knowledge Ops — CMS Data Model Spec

> *The document is the atom. Visibility is the gate. Publish is the sync.*

---

## Purpose

Define the data model, state machine, and API contract for the agent-aware CMS layer that controls the boundary between internal knowledge and public-facing content.

## Canonical Status

This spec originally modeled the CMS as a separate Postgres document store that syncs into a vector store.

That model is now considered **transitional and architecturally broken**.

### Broken assumption

The old shape was:

```text
ingestion -> Ragussy/Qdrant
CMS -> Postgres documents table
publish -> copy Postgres document into public_docs
```

That creates two unrelated knowledge stores:
- the ingested knowledge base
- the CMS document base

The result is that knowledge and content are detached.

### Canonical model going forward

OpenPlanner is the canonical store for:
- ingested documents
- code/config/data artifacts
- live events
- document visibility metadata

The CMS is a **view and mutation layer over OpenPlanner**, not a second database.

### Transitional note

The `documents` table described below is retained only as a compatibility bridge for the current Python service.
It is **not** the target architecture.

The target architecture is:

```text
ingestion -> OpenPlanner documents/events
CMS -> reads and mutates OpenPlanner document metadata
publish/archive -> visibility changes inside OpenPlanner
widget/query -> search OpenPlanner with visibility filters
```

---

## Database Schema

## Transitional Schema

### `documents` table

```sql
CREATE TABLE documents (
    doc_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       TEXT NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    title           TEXT NOT NULL,
    content         TEXT NOT NULL,
    visibility      TEXT NOT NULL DEFAULT 'internal'
                        CHECK (visibility IN ('internal', 'review', 'public', 'archived')),
    source          TEXT NOT NULL DEFAULT 'manual'
                        CHECK (source IN ('manual', 'ai-drafted', 'ingested')),
    source_path     TEXT,                    -- original file path if ingested
    domain          TEXT DEFAULT 'general',
    language        TEXT DEFAULT 'en',
    created_by      TEXT NOT NULL,
    published_by    TEXT,
    published_at    TIMESTAMPTZ,
    last_reviewed_at TIMESTAMPTZ,
    ai_drafted      BOOLEAN NOT NULL DEFAULT false,
    ai_model        TEXT,                    -- which model generated the draft
    ai_prompt_hash  TEXT,                    -- SHA-256 of the prompt for reproducibility
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_tenant ON documents(tenant_id);
CREATE INDEX idx_documents_visibility ON documents(visibility);
CREATE INDEX idx_documents_tenant_visibility ON documents(tenant_id, visibility);
CREATE INDEX idx_documents_domain ON documents(tenant_id, domain);
CREATE INDEX idx_documents_source_path ON documents(source_path) WHERE source_path IS NOT NULL;
```

### `document_versions` table (Phase 2)

```sql
CREATE TABLE document_versions (
    version_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id          UUID NOT NULL REFERENCES documents(doc_id) ON DELETE CASCADE,
    version_num     INTEGER NOT NULL,
    content         TEXT NOT NULL,
    changed_by      TEXT NOT NULL,
    change_summary  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(doc_id, version_num)
);

CREATE INDEX idx_doc_versions_doc ON document_versions(doc_id);
```

These tables remain valid as an implementation bridge, but they no longer define the canonical product architecture.

---

## Canonical OpenPlanner Model

### Documents are events with document semantics

OpenPlanner stores documents as first-class records with:

| Field | Meaning |
|------|---------|
| `project` | lake key (`devel-docs`, `devel-code`, `devel-config`, `devel-data`) |
| `kind` | `docs` \| `code` \| `config` \| `data` |
| `source` | ingest source (`kms-ingestion`, `manual`, `ai-drafted`) |
| `text` | canonical content |
| `meta.visibility` | `internal` \| `review` \| `public` \| `archived` |
| `meta.tags` | domain tags |
| `extra.path` | original workspace path |
| `extra.content_hash` | change detection |

### CMS semantics on top of OpenPlanner

The CMS becomes:
- `list documents` -> filtered OpenPlanner query
- `get document` -> OpenPlanner event/document lookup
- `update content` -> new version / updated record in OpenPlanner
- `publish` -> set `meta.visibility = public`
- `archive` -> set `meta.visibility = archived`
- `draft` -> synthesize through Proxx, store back into OpenPlanner as `source=ai-drafted`

---

## Visibility State Machine

```
                    ┌──────────┐
            ┌──────│ internal  │──────┐
            │      └──────────┘      │
            │    POST /cms/draft      │  ai-drafted
            │    or POST /cms/create   │
            ▼                         ▼
     ┌──────────┐             ┌──────────┐
     │  review  │             │ internal │
     └──────────┘             │(ai-draft)│
       │                      └──────────┘
       │ POST /cms/publish
       ▼
     ┌──────────┐
     │  public  │◄──── visible to Layer 1 widget
     └──────────┘
       │
       │ POST /cms/archive
       ▼
     ┌──────────┐
     │ archived │──── hidden from all, recoverable
     └──────────┘
```

| Transition | Endpoint | Who can trigger | Side effects |
|-----------|----------|----------------|--------------|
| → `internal` | `POST /cms/documents` | Any CMS user | Creates doc row |
| → `internal` (ai) | `POST /cms/draft` | Any CMS user | Creates doc row, `ai_drafted=true` |
| `internal` → `review` | `PATCH /cms/documents/{id}` | Editor, admin | Sets visibility |
| `review` → `public` | `POST /cms/publish/{id}` | Editor, admin | Sets visibility + syncs to Qdrant `public_docs` |
| `review` → `internal` | `PATCH /cms/documents/{id}` | Editor, admin | Rejects draft, sends back |
| `public` → `archived` | `POST /cms/archive/{id}` | Editor, admin | Removes from `public_docs` Qdrant collection |
| `archived` → `public` | `POST /cms/publish/{id}` | Admin | Re-syncs to `public_docs` |
| `archived` → `internal` | `PATCH /cms/documents/{id}` | Admin | Full reset |

---

## Publish Flow (The Critical Path)

```
POST /api/cms/publish/{doc_id}
    │
    ├─ 1. Verify document exists in OpenPlanner and tenant matches
    ├─ 2. Set meta.visibility = 'public'
    ├─ 3. Optionally project into a dedicated `devel-public` / `public_docs` view index
    └─ 4. Return success
```

### Archive Flow

```
POST /api/cms/archive/{doc_id}
    │
    ├─ 1. Verify document exists and is currently public
    ├─ 2. Set meta.visibility = 'archived'
    └─ 3. Return success
```

### Edit-and-Republish Flow

```
PATCH /api/cms/documents/{doc_id} { content: "new content" }
    │
    ├─ 1. If doc is 'public': set visibility = 'review' (unpublish first)
    ├─ 2. Update content, set updated_at = now()
    ├─ 3. Delete old chunks from Qdrant "public_docs" if was public
    └─ 4. User must POST /cms/publish/{id} again to republish
```

---

## AI Draft Flow

```
POST /api/cms/draft
{
    "tenant_id": "devel",
    "topic": "Our onboarding process",
    "tone": "professional",
    "audience": "prospects",
    "source_collections": ["devel_docs"],
    "max_context_chunks": 5
}
    │
    ├─ 1. Search source_collections via OpenPlanner federated search
    ├─ 2. Build prompt: topic + tone + audience + context chunks
    ├─ 3. Call Proxx with system prompt
    ├─ 4. Store draft in OpenPlanner with:
    │      visibility = 'internal'
    │      source = 'ai-drafted'
    │      ai_drafted = true
    └─ 5. Return document id for human review
```

---

## API Surface

| Endpoint | Method | Purpose | Auth |
|----------|--------|---------|------|
| `/api/cms/documents` | GET | List docs, filter by visibility, domain, source | Tenant scope |
| `/api/cms/documents/{id}` | GET | Get single doc | Tenant scope |
| `/api/cms/documents` | POST | Create manual doc | CMS user |
| `/api/cms/documents/{id}` | PATCH | Update content, visibility, metadata | CMS editor |
| `/api/cms/documents/{id}` | DELETE | Soft delete (set visibility=archived) | CMS editor |
| `/api/cms/draft` | POST | AI-generate draft from topic + sources | CMS user |
| `/api/cms/publish/{id}` | POST | Set visibility=public, sync to Qdrant | CMS editor |
| `/api/cms/archive/{id}` | POST | Pull from public, remove from Qdrant | CMS editor |
| `/api/cms/public` | GET | List all public docs (Layer 1 reads this) | Tenant scope |
| `/api/cms/stats` | GET | Counts by visibility, domain | Tenant scope |

---

## Qdrant Collections

## Legacy Collection Model

| Collection | Contents | Fed by | Read by |
|-----------|----------|--------|---------|
| `devel_docs` | All ingested raw docs | `ingest_docs.py` via Ragussy | Layer 3 (knowledge workers) |
| `devel_specs` | Specs and design docs | `ingest_docs.py --collection devel_specs` | Layer 3 |
| `public_docs` | Curated public docs only | CMS publish action | Layer 1 (widget) |

All collections use BGE-M3 embeddings at 1024 dimensions. All points carry `tenant_id` metadata for filtering.

This section is retained for legacy compatibility. The canonical replacement is the role-scoped OpenPlanner lake model defined in:
- `knowledge-ops-role-scoped-lakes.md`
- `knowledge-ops-federated-lakes.md`
- `knowledge-ops-architecture-migration.md`

---

## Pydantic Models

```python
class Document(BaseModel):
    doc_id: str
    tenant_id: str
    title: str
    content: str
    visibility: Literal["internal", "review", "public", "archived"]
    source: Literal["manual", "ai-drafted", "ingested"]
    source_path: str | None
    domain: str
    language: str
    created_by: str
    published_by: str | None
    published_at: datetime | None
    last_reviewed_at: datetime | None
    ai_drafted: bool
    ai_model: str | None
    ai_prompt_hash: str | None
    metadata: dict
    created_at: datetime
    updated_at: datetime

class CreateDocumentPayload(BaseModel):
    title: str
    content: str
    domain: str = "general"
    language: str = "en"
    source_path: str | None = None
    metadata: dict = {}

class DraftRequest(BaseModel):
    tenant_id: str
    topic: str
    tone: str = "professional"
    audience: str = "general"
    source_collections: list[str] = ["devel_docs"]
    max_context_chunks: int = 5

class DocumentListResponse(BaseModel):
    documents: list[Document]
    total: int
    by_visibility: dict[str, int]
```

---

## Files to Create

| File | What |
|------|------|
| `packages/futuresight-kms/python/km_labels/database.py` | Add `documents` table + `document_versions` table to `init_db()` |
| `packages/futuresight-kms/python/km_labels/models.py` | Add `Document`, `CreateDocumentPayload`, `DraftRequest`, `DocumentListResponse` |
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | Full CMS router with all endpoints |

## Files to Reference

| File | Why |
|------|-----|
| `orgs/mojomast/ragussy/backend/app/api/rag.py` | Reuse `_chunk_text()` function and Qdrant upsert pattern |
| `orgs/mojomast/ragussy/backend/app/services/embeddings_service.py` | Reuse `EmbeddingsService` for BGE-M3 embedding |
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Reference for CRUD patterns, tenant validation |
| `packages/futuresight-kms/python/km_labels/database.py` | Existing DB pool and table creation pattern |

---

## Status

Specified 2026-04-01. Blocking questions resolved.
