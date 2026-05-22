# The Lake — Unified Architecture Spec

> *One data lake. Two embedding tiers. Three faces. The corpse of Promethean finally has a spine.*

---

## The Story So Far

You built OpenPlanner as a personal data lake for LLM session archives. It evolved into something you didn't fully realize: a general-purpose event + document store with dual-tier semantic search and compaction. Meanwhile, Promethean scattered related ideas across dozens of docs — semantic store abstractions, functional embedding pipelines, context compaction, retention schedulers, nomic vs qwen embedding strategies, Chroma vs Qdrant driver layers.

None of these pieces know about each other. But they're all the same idea seen from different angles:

**Short things need fast cheap embeddings. Long things need deep accurate embeddings. Related things should be compacted together. The search layer should fuse both tiers.**

That's what OpenPlanner already does. We just need to extend it and point it at the right problems.

---

## What OpenPlanner Actually Is (In Hindsight)

The workspace has been thinking about OpenPlanner as "the thing that archives chat sessions." But look at what it actually implements:

| Capability | What it does | What it actually is |
|-----------|-------------|-------------------|
| Event ingestion | Accepts `EventEnvelopeV1` from any source | **Universal event bus** |
| DuckDB structured store | Events table + compacted_memories table | **Queryable data warehouse** |
| ChromaDB hot collection | Raw event embeddings, fast cheap model | **Instant recall layer** |
| ChromaDB compact collection | Semantic pack embeddings, deeper model | **Deep memory layer** |
| Semantic compaction | Groups related events into packs | **Knowledge graph builder** |
| Dual-tier RRF search | Fuses hot + compact results | **Multi-resolution retrieval** |
| Blob storage | Content-addressed by SHA-256 | **Document store primitive** |
| Job queue | Async imports + compaction | **Processing pipeline** |
| Per-source/kind embedding model resolution | Different models per event type | **Tiered embedding strategy** |

That's not a chat archive. That's a **knowledge lake with tiered recall.**

---

## The Architecture You've Been Circling

### The Three Embedding Tiers

Across the workspace, you've been unconsciously designing three resolution levels:

```
TIER 1: HOT (small, fast, cheap)
  Model: nomic-embed-text or qwen3-embedding:0.6b
  Dimension: 768 (nomic) or variable (qwen3-0.6b)
  Use: Raw events, short messages, chat turns, status updates
  Latency: <50ms per embed
  Cost: Negligible (local Ollama)
  OpenPlanner: CHROMA_COLLECTION (hot)
  Promethean ref: "nomic for small events"

TIER 2: WARM (medium, accurate)
  Model: qwen3-embedding:4b
  Dimension: variable
  Use: Documents, chunks, CMS content, specs
  Latency: <200ms per embed
  Cost: Low (local Ollama, bigger model)
  OpenPlanner: CHROMA_COMPACT_COLLECTION
  Promethean ref: "qwen3-embedding for larger things"

TIER 3: COMPACT (dense, synthesized)
  Model: qwen3-embedding:8b or 4b (per config)
  Dimension: variable
  Use: Semantic packs, compacted memories, knowledge summaries
  Latency: Batch only
  Cost: Low (batched, infrequent)
  OpenPlanner: Semantic compaction output
  Promethean ref: "compaction passes", "context compaction"
```

### The Dual-Tier Search (Already Implemented)

```
Query
  │
  ├─► Embed with hot model → Search hot collection → hot results
  ├─► Embed with compact model → Search compact collection → compact results
  └─► Merge with RRF (reciprocal rank fusion) → final ranked results
```

This is already working in OpenPlanner (`vector-search.ts`). The RRF merge is the key insight — it fuses precision (compact) with recall (hot).

---

## The Extension: Document-Oriented Endpoints

OpenPlanner currently only speaks `EventEnvelopeV1`. For futuresight-kms, we need a **document layer** on top of the event layer.

### New Data Type: `DocumentEnvelopeV1`

```typescript
type DocumentEnvelopeV1 = {
  schema: "openplanner.document.v1";
  id: string;                    // document ID
  ts: string;                    // creation timestamp
  tenant_id: string;             // multi-tenant isolation
  title: string;
  content: string;               // full document text
  visibility: "internal" | "review" | "public" | "archived";
  source: "manual" | "ai-drafted" | "ingested" | "synced";
  source_path?: string;          // original file path
  domain: string;                // topic domain
  language: string;              // ISO 639-1
  tags?: string[];
  metadata: Record<string, unknown>;
  // Embedding state
  embedding_tier: "hot" | "warm" | "compact";
  embedding_model?: string;      // which model was used
  chunk_count?: number;          // how many chunks were created
  // Lifecycle
  published_by?: string;
  published_at?: string;
  ai_drafted?: boolean;
  ai_model?: string;
}
```

### New Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/v1/documents` | Ingest a document. Chunks it, embeds chunks into warm tier, stores metadata in DuckDB. |
| `GET` | `/v1/documents` | List documents, filter by tenant, visibility, domain. |
| `GET` | `/v1/documents/:id` | Get document with content and metadata. |
| `PATCH` | `/v1/documents/:id` | Update content or visibility. Re-chunks and re-embeds if content changes. |
| `DELETE` | `/v1/documents/:id` | Soft delete (set visibility=archived). Removes from search. |
| `POST` | `/v1/documents/:id/publish` | Set visibility=public. Ensures warm-tier embeddings exist. |
| `POST` | `/v1/documents/:id/archive` | Set visibility=archived. Removes from search index. |
| `POST` | `/v1/search/document` | Search documents with tenant + visibility filtering. Hybrid FTS + vector. |

### Why Documents Need a Different Embedding Strategy Than Events

Events are **short** (a chat message, a status update, a Discord post). Documents are **long** (a spec, a README, a blog post). The embedding strategy must differ:

| | Events | Documents |
|--|--------|-----------|
| Chunking | No chunking — each event is one embedding | Chunk at 512 tokens with 64 overlap |
| Embedding model | Hot: nomic-embed-text or qwen3-0.6b | Warm: qwen3-embedding:4b |
| Search tier | Hot + Compact | Warm + Compact |
| Compaction | Events → semantic packs | Documents → section summaries → packs |
| Storage | DuckDB events table | DuckDB documents table |

---

## The Full Picture

```
                    ┌─────────────────────────────────────┐
                    │           INGESTION LAYER            │
                    │                                     │
                    │  POST /v1/events    (short, hot)    │
                    │  POST /v1/documents (long, warm)    │
                    │  POST /v1/blobs     (binary, warm)  │
                    └──────────┬──────────────────────────┘
                               │
                    ┌──────────▼──────────────────────────┐
                    │         EMBEDDING LAYER              │
                    │                                     │
                    │  Per-source/kind/tenant model        │
                    │  resolution:                         │
                    │    events → qwen3-0.6b (hot)        │
                    │    documents → qwen3-4b (warm)      │
                    │    compaction → qwen3-8b (deep)     │
                    └──────────┬──────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
     ┌────────────┐   ┌──────────────┐  ┌─────────────┐
     │   HOT      │   │    WARM      │  │  COMPACT    │
     │ ChromaDB   │   │   ChromaDB   │  │  ChromaDB   │
     │ collection │   │  collection  │  │  collection │
     │ nomic/0.6b │   │   qwen3-4b   │  │  qwen3-8b   │
     │ raw events │   │  doc chunks  │  │  sem. packs │
     └──────┬─────┘   └──────┬───────┘  └──────┬──────┘
            │                │                  │
            └────────────────┼──────────────────┘
                             │
                    ┌────────▼───────────────────────────┐
                    │      STRUCTURED STORE               │
                    │      DuckDB (default)               │
                    │      or MongoDB (scale)             │
                    │                                     │
                    │  events │ documents │ compacted_    │
                    │         │           │ memories      │
                    └────────┬───────────────────────────┘
                             │
                    ┌────────▼───────────────────────────┐
                    │        SEARCH LAYER                 │
                    │                                     │
                    │  POST /v1/search/fts               │
                    │  POST /v1/search/vector            │
                    │  POST /v1/search/document          │
                    │                                     │
                    │  RRF fusion across all tiers        │
                    │  Tenant + visibility filtering      │
                    └────────┬───────────────────────────┘
                             │
              ┌──────────────┼──────────────────┐
              ▼              ▼                  ▼
     ┌────────────┐  ┌─────────────┐  ┌──────────────┐
     │  Layer 1   │  │  Layer 2    │  │  Layer 3     │
     │  Widget    │  │  CMS        │  │  Knowledge   │
     │  (public)  │  │  (curation) │  │  Worker      │
     └────────────┘  └─────────────┘  └──────────────┘
```

---

## The Compaction Pipeline (Extended)

The existing semantic compaction works on events. Extend it for documents:

### Document Compaction Flow

```
1. Load all documents with visibility != 'archived'
2. For each uncompacted document:
   a. Chunk the document (512 tokens, 64 overlap)
   b. Embed each chunk with warm model (qwen3-4b)
   c. Store chunks in warm collection with metadata:
      { tenant_id, doc_id, chunk_index, visibility }
   d. Find semantically related chunks across ALL documents
   e. Build a "section pack" from related chunks
   f. Embed the pack with deep model (qwen3-8b)
   g. Store pack in compact collection
3. For public documents only:
   a. Build a "public pack" from all public chunks
   b. Store in a separate collection for Layer 1 widget
```

### The Big/Small Block Distinction

This is what you were circling with "big blocks vs small blocks":

| Block Type | Size | Embedding | Search Use |
|-----------|------|-----------|-----------|
| **Atom** | Single event (chat message, status) | Hot: nomic/qwen3-0.6b | "What did I say about X yesterday?" |
| **Chunk** | 512-token segment of a document | Warm: qwen3-4b | "Find the section about deployment" |
| **Document** | Full document metadata + all chunks | Warm: per-chunk | "What documents mention X?" |
| **Pack** | Semantically related atoms/chunks compacted | Deep: qwen3-8b | "What do I know about X across everything?" |
| **Public Pack** | Only public-facing content compacted | Deep: qwen3-8b | Layer 1 widget retrieval |

---

## Multi-Tenancy in the Lake

OpenPlanner currently has no tenant concept. Add it:

### Tenant-Scoped Collections

Instead of one ChromaDB collection per tier, use **namespace-per-tenant**:

| Tier | Collection Pattern | Example |
|------|-------------------|---------|
| Hot | `{tenant_id}_hot` | `devel_hot` |
| Warm | `{tenant_id}_warm` | `devel_warm` |
| Compact | `{tenant_id}_compact` | `devel_compact` |
| Public | `{tenant_id}_public` | `devel_public` |

Or use **metadata filtering** (simpler, same code path):

| Tier | Collection | Metadata Filter |
|------|-----------|----------------|
| Hot | `hot` (shared) | `where: { tenant_id: "devel" }` |
| Warm | `warm` (shared) | `where: { tenant_id: "devel", visibility: "public" }` |
| Compact | `compact` (shared) | `where: { tenant_id: "devel" }` |

**Recommendation**: Metadata filtering. Same as the existing OpenPlanner pattern. Add `tenant_id` to every point's metadata. Filter at query time. This matches what the Promethean semantic store architecture already spec'd.

### Visibility-Aware Search

The document search endpoint enforces visibility:

```typescript
POST /v1/search/document
{
  query: "deployment",
  tenant_id: "devel",
  visibility_filter: ["public"],       // Layer 1: public only
  // or
  visibility_filter: ["internal", "review", "public"],  // Layer 3: everything
  limit: 10,
  search_type: "hybrid"  // fts + vector + rrf
}
```

---

## Replacing Ragussy + Qdrant

Here's the honest assessment:

| Ragussy Feature | OpenPlanner Equivalent | Gap |
|----------------|----------------------|-----|
| Document ingestion | `POST /v1/documents` (new) | Needs chunking logic |
| BGE-M3 dense+sparse embedding | qwen3-4b (warm) + qwen3-0.6b (hot) | Different model, same concept. No sparse vectors yet. |
| Qdrant vector search | ChromaDB vector search | Already working. ChromaDB metadata filtering replaces Qdrant. |
| Hybrid retrieval (dense/hybrid/hybrid_rerank) | Dual-tier RRF search | Already working. Different approach, same goal. |
| OpenAI-compatible chat | **Not in OpenPlanner** | Keep Ragussy for chat inference, or route through proxx |
| llama.cpp lifecycle | **Not in OpenPlanner** | Keep Ragussy for model serving, or use proxx directly |
| Chat UI (Next UI) | **Not in OpenPlanner** | Build new UI, or keep Ragussy frontend for chat |
| Shibboleth handoff | **Not in OpenPlanner** | Add handoff endpoint |

### The Honest Split

**OpenPlanner becomes**: the data lake, search, ingestion, and compaction layer.
**Ragussy becomes** (or is replaced by): proxx for LLM inference. The "RAG" part of Ragussy is what OpenPlanner does better.
**The chat UI**: lives in the shared `@workspace/chat-ui` library, talks to both.

```
BEFORE (current):
  Widget → Ragussy (/api/ragussy/chat) → Qdrant search → LLM → answer
  CMS    → Ragussy (/api/rag/search) → Qdrant search → results
  CMS    → Ragussy (/v1/chat/completions) → llama.cpp → draft

AFTER (with OpenPlanner):
  Widget → OpenPlanner (/v1/search/document?visibility=public) → proxx (/v1/chat/completions) → answer
  CMS    → OpenPlanner (/v1/search/document) → results
  CMS    → proxx (/v1/chat/completions) → draft
  Events → OpenPlanner (/v1/events) → hot tier
  Compaction → OpenPlanner (/v1/jobs/compact/semantic) → compact tier
```

---

## What Changes in the Existing Specs

| Spec | Change |
|------|--------|
| `knowledge-ops-integration.md` | Replace Ragussy as search backend with OpenPlanner. Keep proxx for LLM inference. |
| `knowledge-ops-cms-data-model.md` | CMS `POST /publish` calls OpenPlanner `/v1/documents/:id/publish` instead of Ragussy `/api/rag/ingest/text` |
| `knowledge-ops-deployment.md` | Add OpenPlanner service to docker-compose. Remove Qdrant service (ChromaDB replaces it). |
| `knowledge-ops-platform-stack-architecture.md` | Rename "Ragussy" layer to "OpenPlanner" for search/ingestion. Keep proxx for inference. |
| `knowledge-ops-chat-widget-layers.md` | Widget talks to OpenPlanner search endpoint with visibility filter, not Ragussy chat endpoint. |

---

## Files to Create

| File | Purpose |
|------|---------|
| `services/openplanner/src/routes/v1/documents.ts` | Document CRUD + publish + archive endpoints |
| `services/openplanner/src/lib/documents.ts` | Document storage in DuckDB, chunking, warm-tier embedding |
| `services/openplanner/src/lib/chunking.ts` | 512-token chunking with 64-token overlap |
| `services/openplanner/src/routes/v1/search.ts` | Extend with `POST /v1/search/document` (tenant + visibility filtering) |
| `services/openplanner/src/lib/types.ts` | Add `DocumentEnvelopeV1` type |
| `services/openplanner/src/lib/config.ts` | Add `WARM_EMBED_MODEL`, `WARM_EMBED_DIM` config |
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | CMS router that calls OpenPlanner for search + publish |

## Files to Reference

| File | Why |
|------|-----|
| `services/openplanner/src/lib/duckdb.ts` | Existing DuckDB schema — add documents table |
| `services/openplanner/src/lib/chroma.ts` | Existing ChromaDB integration — add warm collection |
| `services/openplanner/src/lib/embeddings.ts` | Existing Ollama embedding — add warm model |
| `services/openplanner/src/lib/embedding-models.ts` | Per-source/kind model resolution — add document kind |
| `services/openplanner/src/lib/semantic-compaction.ts` | Existing compaction — extend for documents |
| `services/openplanner/src/lib/vector-search.ts` | Existing RRF merge — extend for warm tier |
| `services/openplanner/src/routes/v1/events.ts` | Pattern for ingestion endpoints |
| `orgs/octave-commons/promethean/docs/hacks/inbox/semantic-store-architecture.md` | Driver abstraction pattern — use for ChromaDB |
| `reconstitute/docs/notes/reconstitute/reconstitute-context-compaction.md` | Context compaction pattern — use for document packs |

---

## What This Means for the Demo

The demo seed script now:

1. Creates `devel` tenant in OpenPlanner (or km_labels, which calls OpenPlanner)
2. Ingests `docs/` as events into hot tier via `POST /v1/events`
3. Ingests `specs/` + key READMEs as documents into warm tier via `POST /v1/documents`
4. Runs compaction job → builds semantic packs in compact tier
5. Publishs 2 documents → available in public tier for widget
6. Widget searches `POST /v1/search/document?visibility=public` → gets answers from public packs
7. CMS shows all documents, lets worker draft new ones, publish to public tier

---

## Status

Specified 2026-04-01. Total creative freedom engaged. The lake is the spine.
