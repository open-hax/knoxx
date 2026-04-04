# Knowledge Ops — Integration Spec

> *How the pieces talk to each other. Auth, routing, error handling, retry.*

---

## Purpose

Define the integration contracts between the four services (Ragussy, km_labels, Shibboleth, Qdrant) and how they compose into the five-layer stack.

---

## Service Map

```
┌─────────────────────────────────────────────────────────────┐
│                    NGINX (port 80/443)                       │
│  Routes: / → Ragussy FE, /api/* → services, /stack → landing│
└──────┬──────────┬──────────┬──────────┬──────────────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
   Ragussy     km_labels  Shibboleth  Qdrant
   :8000       :3002      :8788       :6333
```

---

## Integration Contracts

### 1. Ragussy → Qdrant (existing, working)

| Contract | Detail |
|----------|--------|
| Direction | Ragussy reads/writes Qdrant |
| Endpoint | `QDRANT_URL` env var (default `http://qdrant:6333`) |
| Operations | Create collection, upsert points, search, delete points |
| Auth | None (internal network only) |
| Collections | `devel_docs`, `devel_specs`, `public_docs` (added by CMS) |

**Status**: Working. No changes needed.

### 2. km_labels → Ragussy (new)

| Contract | Detail |
|----------|--------|
| Direction | km_labels calls Ragussy for AI drafts and embedding |
| Endpoints | `POST /api/rag/search` (retrieve context for drafts), `POST /v1/chat/completions` (generate drafts) |
| Auth | `RAGUSSY_API_KEY` header |
| Config | `RAGUSSY_BASE_URL` env var (default `http://ragussy:8000`) |

**Used by**: CMS draft generation, CMS publish (embedding)

**Error handling**: If Ragussy is down, draft returns 503. Publish can retry with exponential backoff (max 3 retries).

### 3. km_labels → Qdrant (new)

| Contract | Detail |
|----------|--------|
| Direction | km_labels writes to Qdrant during CMS publish/archive |
| Endpoint | `QDRANT_URL` env var |
| Operations | Upsert points (publish), delete points by filter (archive) |
| Auth | None (internal network) |
| Collections | `public_docs` only (Ragussy owns `devel_docs`/`devel_specs`) |

**Used by**: CMS publish flow, CMS archive flow

**Why km_labels writes directly to Qdrant instead of going through Ragussy**: The publish flow needs to upsert chunks with specific metadata (visibility, doc_id). Going through Ragussy's ingest API would add coupling and lose control over metadata. km_labels reuses Ragussy's `EmbeddingsService` for embedding but writes directly to Qdrant for the upsert.

### 4. Ragussy → Shibboleth (existing, one-directional)

| Contract | Detail |
|----------|--------|
| Direction | Ragussy exports conversations to Shibboleth |
| Endpoint | `POST /api/shibboleth/handoff` in Ragussy → Shibboleth control plane |
| Auth | `SHIBBOLETH_BASE_URL` env var |
| Payload | `SessionHandoff` (conversation + metadata) |

**Status**: Exists. One-directional. Shibboleth doesn't write back to km_labels yet.

### 5. Shibboleth → km_labels (new, Phase 2)

| Contract | Detail |
|----------|--------|
| Direction | Shibboleth writes labeled data back to km_labels |
| Endpoint | `POST /api/km-labels` in km_labels |
| Auth | `KM_LABELS_API_KEY` header |
| Payload | `CreateKmLabelPayload` (matches existing schema) |

**Used by**: After SME completes review in Shibboleth chat lab, labels are persisted in km_labels for SFT/RLHF export.

### 6. Widget → km_labels proxy (new)

| Contract | Detail |
|----------|--------|
| Direction | Widget calls km_labels `/api/widget/*` proxy |
| Endpoints | `GET /api/widget/config`, `POST /api/widget/chat`, `GET /api/widget/review/next` |
| Auth | Tenant ID embedded in widget config + API key |
| Enforcement | Proxy forces `collection=public_docs` for Layer 1. No other collection accessible. |

---

## Auth Propagation

```
Browser (widget)
    │
    │ Headers: X-Tenant-Id, Authorization: Bearer <api_key>
    │
    ▼
Nginx
    │
    │ Resolves tenant from X-Tenant-Id or subdomain
    │ Passes through to upstream
    │
    ▼
km_labels (/api/widget/chat)
    │
    │ Validates tenant_id exists
    │ Validates API key for tenant
    │ Forces collection = "public_docs"
    │
    ▼
Ragussy (/api/ragussy/chat)
    │
    │ Receives: { message, collection: "public_docs" }
    │ Searches Qdrant with collection filter
    │ Returns: { answer, sources }
    │
    ▼
km_labels → Browser
    │
    │ Returns: { answer, sources, model, collection }
```

### Fail-closed rules

- No `X-Tenant-Id` → 401
- `X-Tenant-Id` not in database → 403
- Widget trying to access `devel_docs` → 403 (only `public_docs` allowed)
- Missing API key → 401

---

## Nginx Routing

```nginx
# Widget API proxy
location /api/widget/ {
    proxy_pass http://km-labels:3002/api/widget/;
    proxy_set_header X-Tenant-Id $http_x_tenant_id;
    proxy_set_header X-Real-IP $remote_addr;
}

# CMS API proxy
location /api/cms/ {
    proxy_pass http://km-labels:3002/api/cms/;
    proxy_set_header X-Tenant-Id $http_x_tenant_id;
}

# KM Labels API
location /api/km-labels/ {
    proxy_pass http://km-labels:3002/api/km-labels/;
}

# Export API
location /api/export/ {
    proxy_pass http://km-labels:3002/api/export/;
}

# Health checks (existing)
location /health/ragussy { proxy_pass http://ragussy-backend:8000/health; }
location /health/km-labels { proxy_pass http://km-labels:3002/health; }
location /health/qdrant { proxy_pass http://qdrant:6333/healthz; }

# Ragussy API (existing)
location /api/ {
    proxy_pass http://ragussy-backend:8000/api/;
}

# Ragussy OpenAI compat (existing)
location /v1/ {
    proxy_pass http://ragussy-backend:8000/v1/;
}
```

---

## Environment Variables (complete set)

### km_labels service

```env
# Database
DATABASE_URL=postgresql://kms:kms@postgres:5432/futuresight_kms

# Ragussy (for AI drafts + embedding)
RAGUSSY_BASE_URL=http://ragussy:8000
RAGUSSY_API_KEY=change-me-ragussy

# Qdrant (for CMS publish)
QDRANT_URL=http://qdrant:6333

# Shibboleth (for review integration)
SHIBBOLETH_BASE_URL=http://shibboleth-backend:8788

# Auth
KM_LABELS_API_KEY=change-me-km-labels

# Embedding
EMBED_MODEL=BAAI/bge-m3
EMBED_DIM=1024
```

---

## Docker Compose Additions

The existing `services/futuresight-kms/docker-compose.yml` needs these changes:

1. **km_labels** service needs `RAGUSSY_BASE_URL` and `QDRANT_URL` env vars (already partially present via `x-shared-env`)
2. **km_labels** service needs access to Ragussy's `EmbeddingsService` — either by importing the Python module (if co-located) or by calling Ragussy's `/api/rag/ingest/text` endpoint during publish
3. **Nginx** needs new location blocks for `/api/widget/` and `/api/cms/`

**Decision**: km_labels calls Ragussy's `/api/rag/ingest/text` endpoint during publish rather than importing `EmbeddingsService` directly. This keeps the services decoupled and avoids duplicating embedding code. km_labels sends the document text + metadata to Ragussy, which handles chunking + embedding + Qdrant upsert.

**Revised publish flow**:

```
POST /api/cms/publish/{doc_id}
    ├─ 1. Set visibility = 'public' in Postgres
    ├─ 2. POST to Ragussy /api/rag/ingest/text {
    │        text: doc.content,
    │        source: doc.source_path or doc.doc_id,
    │        collection: "public_docs",
    │        metadata: { tenant_id, doc_id, visibility: "public" }
    │      }
    └─ 3. Return success
```

This is simpler and reuses Ragussy's existing chunking + embedding pipeline.

---

## Error Handling

| Failure | Recovery |
|---------|---------|
| Ragussy down during draft | Return 503, "AI drafting unavailable" |
| Ragussy down during publish | Return 503, retry manually or queue for later |
| Qdrant down during search | Return 503 with cached/fallback response |
| Postgres down | Return 503, all endpoints fail |
| Invalid tenant_id | 403 immediately, no retry |
| Embedding model mismatch | Detect via dimension check on first search, re-index if needed |

---

## Health Check Chain

```
GET /health/ragussy    → Ragussy /health           → 200 if llama + Qdrant reachable
GET /health/km-labels  → km_labels /health          → 200 if Postgres reachable
GET /health/qdrant     → Qdrant /healthz            → 200 if Qdrant running
GET /health/shibboleth → Shibboleth control plane   → 200 if port 8788 reachable
```

---

## Status

Specified 2026-04-01. Blocking questions resolved.
