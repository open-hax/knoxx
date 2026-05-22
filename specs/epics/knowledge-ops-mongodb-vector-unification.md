# The Lake — MongoDB Vector Unification Spec

> *One database. Structured storage + vector search + full-text search. No sidecar vector stores.*

---

## What Happened

In September 2025, MongoDB shipped `$vectorSearch` and `$search` for Community Edition and Enterprise Server. Not Atlas-only. Self-managed. Free under SSPL. Same APIs as Atlas. Public preview as of 8.2.

This means: **MongoDB can now be the only database.** Structured storage, full-text search, AND vector similarity search, all in one query language, one connection string, one transaction boundary.

The current OpenPlanner stack is three databases duct-taped together:

| Current | What it does | Replace with |
|---------|-------------|-------------|
| DuckDB | Events table, compacted_memories table, FTS | MongoDB collections + `$search` |
| ChromaDB | Vector embeddings, hot + compact collections | MongoDB `$vectorSearch` + vector indexes |
| Blob store | Content-addressed binary storage | GridFS or MongoDB binary storage |

All three → **one MongoDB instance with mongot.**

---

## What It's Called

**MongoDB Vector Search** (self-managed). The feature adds:
- `$vectorSearch` — semantic similarity search in aggregation pipelines
- `$search` — full-text keyword search with fuzzy matching
- `$searchMeta` — metadata and faceting for search results
- `mongot` — separate search binary that handles indexing (runs alongside mongod)

Available in:
- **Community Edition 8.2+** — free, SSPL, public preview
- **Enterprise Server 8.2+** — will be paid subscription after preview
- **Atlas** — GA, fully managed (we don't need this)

---

## Architecture

### Before (OpenPlanner today)

```
OpenPlanner (Node.js/Fastify)
    │
    ├──► DuckDB (structured: events, compacted_memories, FTS)
    ├──► ChromaDB (vectors: hot collection, compact collection)
    ├──► Ollama (embeddings: qwen3-embedding:0.6b, :4b, :8b)
    └──► File system (blobs: openplanner-lake/blobs/sha256/)
```

### After (MongoDB Vector Unification)

```
OpenPlanner (Node.js/Fastify)
    │
    ├──► MongoDB 8.2 (everything)
    │       ├── events collection (structured + text search)
    │       ├── documents collection (structured + text search + vector search)
    │       ├── compacted_memories collection (structured + vector search)
    │       ├── hot_vectors collection (vector search, 768d nomic)
    │       ├── warm_vectors collection (vector search, variable qwen3-4b)
    │       ├── compact_vectors collection (vector search, variable qwen3-8b)
    │       └── blobs (GridFS or binary collection)
    │
    ├──► mongot (search server, sidecar to mongod)
    │
    └──► Ollama (embeddings only — no storage)
```

**DuckDB is gone. ChromaDB is gone. One MongoDB instance replaces both.**

---

## Why This Works

### What MongoDB 8.2 gives you

| Capability | How | Current equivalent |
|-----------|-----|-------------------|
| Structured storage | Native MongoDB collections | DuckDB tables |
| Full-text search | `$search` with fuzzy matching, scoring | DuckDB FTS extension (fragile) |
| Vector search | `$vectorSearch` with cosine similarity, HNSW index | ChromaDB collections |
| Hybrid search | `$search` + `$vectorSearch` + RRF merge in aggregation pipeline | OpenPlanner's manual RRF in `vector-search.ts` |
| Metadata filtering | `$vectorSearch` with pre-filter on any field | ChromaDB `where` filter |
| Transactional consistency | Native MongoDB transactions | Never existed (DuckDB + ChromaDB were eventually consistent) |
| Schema validation | JSON Schema validation on collections | Manual validation in TypeScript |
| Indexes | Compound indexes, TTL indexes, text indexes, vector indexes | DuckDB indexes + ChromaDB's limited indexing |
| Replica set | Built-in replication and failover | None (single-file DuckDB, single-container ChromaDB) |

### What we lose

| Loss | Impact | Mitigation |
|------|--------|------------|
| DuckDB's columnar analytics | OLAP queries (aggregations over large event sets) are slower | MongoDB aggregation pipeline handles this. Not as fast for pure analytics but adequate for our scale. |
| ChromaDB's simplicity | MongoDB is more complex to operate | Docker Compose handles this. mongot is a separate binary. |
| DuckDB's zero-config | MongoDB needs replica set mode | Docker Compose init script handles replica set initialization. One extra step. |
| Zero-dependency embedding model resolution | ChromaDB had built-in embedding functions | Ollama stays as embedding provider. No change — OpenPlanner already calls Ollama directly. |

### What we gain

| Gain | Impact |
|------|--------|
| One connection string | No sync between DuckDB and ChromaDB. No consistency bugs. |
| Transactional writes | Event + vector inserted atomically. No partial states. |
| Metadata filtering on vectors | Filter by tenant_id, visibility, domain AT THE VECTOR INDEX LEVEL, not post-filter. |
| Hybrid search in one query | `$search` + `$vectorSearch` + RRF in a single aggregation pipeline. |
| Operational simplicity | One Docker service instead of three. One backup strategy. One monitoring target. |
| Upgrade path to Atlas | If futuresight grows, same code works on Atlas with zero changes. |

---

## Data Model

### `events` collection

```javascript
{
  _id: ObjectId(),
  schema: "openplanner.event.v1",
  id: "uuid",                    // original event ID
  ts: ISODate("2026-04-01T..."),
  tenant_id: "devel",            // NEW: multi-tenant
  source: "chatgpt-export",
  kind: "message",
  project: null,
  session: "session-id",
  message: "msg-id",
  role: "user",
  author: "user@example.com",
  model: null,
  tags: ["import"],
  text: "The actual event content...",
  embedding: [0.123, -0.456, ...],  // 768d from nomic/qwen3-0.6b
  attachments: [{ sha256: "...", mime: "text/plain" }],
  extra: {},
  // Indexes
  _created_at: ISODate()
}
```

**Indexes**:
```javascript
// Compound: tenant + time
db.events.createIndex({ tenant_id: 1, ts: -1 })

// Text search
db.events.createSearchIndex("events_text", "search", {
  mappings: { dynamic: false, fields: { text: { type: "string" }, tags: { type: "string" } } }
})

// Vector search (hot tier)
db.events.createSearchIndex("events_vector_hot", "vectorSearch", {
  fields: [{
    type: "vector",
    path: "embedding",
    numDimensions: 768,
    similarity: "cosine"
  }]
})
```

### `documents` collection

```javascript
{
  _id: ObjectId(),
  schema: "openplanner.document.v1",
  id: "uuid",
  ts: ISODate("2026-04-01T..."),
  tenant_id: "devel",
  title: "Deploying Futuresight KMS",
  content: "Full document text...",
  visibility: "public",           // internal | review | public | archived
  source: "ingested",
  source_path: "services/futuresight-kms/README.md",
  domain: "services",
  language: "en",
  tags: ["deployment", "docker"],
  metadata: {},
  // Embedding state
  ai_drafted: false,
  ai_model: null,
  published_by: "admin",
  published_at: ISODate(),
  _created_at: ISODate(),
  _updated_at: ISODate()
}
```

### `document_chunks` collection

```javascript
{
  _id: ObjectId(),
  document_id: "uuid",           // parent document
  tenant_id: "devel",
  chunk_index: 0,
  text: "This chunk covers the Docker Compose setup...",
  token_count: 512,
  embedding: [0.234, -0.567, ...],  // variable dims from qwen3-4b
  visibility: "public",          // inherited from parent document
  domain: "services",
  _created_at: ISODate()
}
```

**Indexes**:
```javascript
// Tenant + document
db.document_chunks.createIndex({ tenant_id: 1, document_id: 1 })

// Visibility filtering (for Layer 1 vs Layer 3)
db.document_chunks.createIndex({ tenant_id: 1, visibility: 1 })

// Text search on chunks
db.document_chunks.createSearchIndex("chunks_text", "search", {
  mappings: { dynamic: false, fields: { text: { type: "string" } } }
})

// Vector search (warm tier)
db.document_chunks.createSearchIndex("chunks_vector_warm", "vectorSearch", {
  fields: [{
    type: "vector",
    path: "embedding",
    numDimensions: 1024,          // qwen3-4b dimension
    similarity: "cosine"
  }]
})
```

### `compacted_memories` collection

```javascript
{
  _id: ObjectId(),
  schema: "openplanner.compacted.v1",
  id: "uuid",
  ts: ISODate(),
  tenant_id: "devel",
  source: "semantic-compaction",
  kind: "pack",
  project: null,
  session: null,
  seed_id: "event-uuid",         // the seed event
  member_count: 12,
  char_count: 4500,
  embedding_model: "qwen3-embedding:8b",
  text: "Compacted markdown summary of related events...",
  embedding: [0.345, -0.678, ...],  // variable dims from qwen3-8b
  members: ["event-uuid-1", "event-uuid-2", ...],
  extra: {},
  _created_at: ISODate()
}
```

**Indexes**:
```javascript
// Vector search (compact tier)
db.compacted_memories.createSearchIndex("compact_vector", "vectorSearch", {
  fields: [{
    type: "vector",
    path: "embedding",
    numDimensions: 4096,          // qwen3-8b dimension
    similarity: "cosine"
  }]
})
```

### `blobs` collection (GridFS or custom)

Use MongoDB's built-in GridFS for binary storage. Content-addressed by SHA-256 filename.

---

## Search Queries

### Vector search with tenant + visibility filter

```javascript
db.document_chunks.aggregate([
  {
    $vectorSearch: {
      index: "chunks_vector_warm",
      path: "embedding",
      queryVector: queryEmbedding,
      numCandidates: 200,
      limit: 10,
      filter: {
        tenant_id: { $eq: "devel" },
        visibility: { $eq: "public" }    // Layer 1: public only
      }
    }
  },
  {
    $project: {
      text: 1,
      document_id: 1,
      chunk_index: 1,
      visibility: 1,
      score: { $meta: "vectorSearchScore" }
    }
  }
])
```

### Hybrid search (text + vector + RRF)

```javascript
// Vector search
const vectorResults = db.document_chunks.aggregate([
  { $vectorSearch: { index: "chunks_vector_warm", path: "embedding",
      queryVector: queryEmbedding, numCandidates: 200, limit: 20,
      filter: { tenant_id: { $eq: "devel" }, visibility: { $eq: "public" } } } },
  { $project: { text: 1, document_id: 1, score: { $meta: "vectorSearchScore" } } }
])

// Text search
const textResults = db.document_chunks.aggregate([
  { $search: { index: "chunks_text", text: { query: queryText, path: "text" } } },
  { $match: { tenant_id: "devel", visibility: "public" } },
  { $limit: 20 },
  { $project: { text: 1, document_id: 1, score: { $meta: "searchScore" } } }
])

// RRF merge (same as OpenPlanner's existing vector-search.ts)
const merged = rrfMerge(vectorResults, textResults, k = 60)
```

### Tenant-scoped event search (hot tier)

```javascript
db.events.aggregate([
  { $vectorSearch: { index: "events_vector_hot", path: "embedding",
      queryVector: queryEmbedding, numCandidates: 200, limit: 10,
      filter: { tenant_id: { $eq: "devel" } } } },
  { $project: { text: 1, source: 1, ts: 1, score: { $meta: "vectorSearchScore" } } }
])
```

---

## Docker Compose

```yaml
services:
  mongodb:
    image: mongodb/mongodb-community-server:8.2.0-ubi9
    command: mongod --replSet rs0
    ports:
      - "${MONGO_PORT:-27017}:27017"
    volumes:
      - openplanner-mongo:/data/db
    healthcheck:
      test: mongosh --eval "db.adminCommand('ping')" --quiet
      interval: 10s
      retries: 5
      start_period: 30s
    networks:
      - default

  mongot:
    image: mongodb/mongodb-community-search:0.53.1
    ports:
      - "27028:27028"
    depends_on:
      mongodb:
        condition: service_healthy
    networks:
      - default

  mongo-init:
    image: mongodb/mongodb-community-server:8.2.0-ubi9
    depends_on:
      mongodb:
        condition: service_healthy
    entrypoint: |
      mongosh --host mongodb --eval '
        rs.initiate({ _id: "rs0", members: [{ _id: 0, host: "mongodb:27017" }] })
      ' || true
    networks:
      - default

  openplanner:
    build: .
    depends_on:
      mongodb:
        condition: service_healthy
      mongot:
        condition: service_started
      mongo-init:
        condition: service_completed_successfully
    environment:
      MONGO_URL: mongodb://mongodb:27017/openplanner?replicaSet=rs0
      OLLAMA_BASE_URL: ${OLLAMA_BASE_URL:-http://127.0.0.1:8789}
    networks:
      - default

volumes:
  openplanner-mongo:
```

**No ChromaDB service. No DuckDB volume. Just MongoDB + mongot.**

---

## Migration Path

### Phase 1: Add MongoDB alongside existing stack

1. Add `mongodb` + `mongot` + `mongo-init` services to docker-compose
2. Add MongoDB connection module to OpenPlanner (`lib/mongodb-search.ts`)
3. Create collections and search indexes
4. Dual-write: events go to both DuckDB+ChromaDB AND MongoDB
5. Verify MongoDB search results match ChromaDB results

### Phase 2: Switch reads to MongoDB

1. Point `/v1/search/fts` to MongoDB `$search`
2. Point `/v1/search/vector` to MongoDB `$vectorSearch`
3. Keep DuckDB+ChromaDB as write target (fallback)
4. Compare search quality in production

### Phase 3: Drop old stores

1. Stop writing to DuckDB and ChromaDB
2. Remove DuckDB and ChromaDB from docker-compose
3. Remove `lib/duckdb.ts`, `lib/chroma.ts`, `plugins/duckdb.ts`, `plugins/chroma.ts`
4. Migrate existing DuckDB data to MongoDB (one-time export/import)
5. Migrate existing ChromaDB vectors to MongoDB (re-embed or copy)

### Data migration script

```javascript
// migrate-duckdb-to-mongo.js
// 1. Export DuckDB events to JSONL
// 2. Import JSONL into MongoDB events collection
// 3. Export ChromaDB vectors
// 4. Attach vectors to MongoDB documents
// 5. Create search indexes
// 6. Verify counts match
```

---

## Impact on Other Specs

| Spec | Change |
|------|--------|
| `knowledge-ops-the-lake.md` | Update: MongoDB replaces DuckDB+ChromaDB. Same tier model. |
| `knowledge-ops-integration.md` | Update: OpenPlanner uses MongoDB, not ChromaDB. No Qdrant. |
| `knowledge-ops-deployment.md` | Update: MongoDB+mongot services replace Qdrant+ChromaDB. |
| `knowledge-ops-cms-data-model.md` | Update: km_labels can use MongoDB too (or keep Postgres — their choice). |
| `knowledge-ops-multi-tenant-control-plane.md` | Benefit: metadata filtering on vector search at index level. |

---

## What About km_labels?

The km_labels service currently uses PostgreSQL. Two options:

**Option A: Keep Postgres for km_labels, MongoDB for OpenPlanner**
- Pros: No migration risk for km_labels. Clean separation of concerns.
- Cons: Two databases to operate. Labels + documents in different stores.

**Option B: Migrate km_labels to MongoDB too**
- Pros: One database for everything. Labels, documents, events, vectors.
- Cons: km_labels Python code uses asyncpg (Postgres driver). Need to rewrite to pymongo/motor.

**Recommendation**: Option A for Phase 1. Revisit Option B when the label schema stabilizes. The integration between km_labels and OpenPlanner stays HTTP-based regardless.

---

## OpenPlanner Code Changes

### Files to modify

| File | Change |
|------|--------|
| `src/lib/config.ts` | Add `MONGO_URL` env var. Remove `CHROMA_URL`, `DUCKDB_PATH`. |
| `src/lib/mongodb.ts` | Already exists! Extend with vector search functions, search index creation. |
| `src/plugins/mongodb.ts` | Already exists! Make it the primary plugin (not optional). |
| `src/routes/v1/search.ts` | Rewrite to use MongoDB `$vectorSearch` and `$search` instead of ChromaDB. |
| `src/routes/v1/events.ts` | Write events to MongoDB (already does this in MongoDB mode). |
| `src/lib/vector-search.ts` | Rewrite RRF merge to use MongoDB aggregation results. |
| `src/lib/semantic-compaction.ts` | Store packs in MongoDB `compacted_memories` collection. |
| `src/lib/duckdb.ts` | Deprecate. Keep for migration script only. |
| `src/lib/chroma.ts` | Deprecate. Keep for migration script only. |
| `src/plugins/duckdb.ts` | Remove after migration. |
| `src/plugins/chroma.ts` | Remove after migration. |

### Files to create

| File | Purpose |
|------|--------|
| `src/lib/mongodb-search.ts` | Vector search, text search, hybrid search functions using MongoDB aggregation |
| `src/lib/mongodb-indexes.ts` | Search index creation and management |
| `scripts/migrate-to-mongodb.ts` | One-time migration from DuckDB+ChromaDB to MongoDB |

---

## The Honest Trade-Off

| | DuckDB + ChromaDB | MongoDB Vector Search |
|--|-------------------|----------------------|
| Operational complexity | 2 databases, 2 connection strings, sync issues | 1 database, 1 connection string, mongot sidecar |
| Query power | SQL + ChromaDB API (limited) | MongoDB aggregation (powerful) |
| Vector search quality | ChromaDB HNSW (good) | MongoDB HNSW (same algorithm) |
| Full-text search | DuckDB FTS (basic, fragile) | MongoDB `$search` (Lucene-grade) |
| Transactional consistency | None (two separate stores) | Full ACID |
| License cost | Free (MIT/Apache) | Free (SSPL) — note: SSPL is more restrictive than MIT |
| Community/ecosystem | Small (DuckDB growing, ChromaDB niche) | Massive (MongoDB is everywhere) |
| Cloud migration path | Manual (different vector DB providers) | Same code works on Atlas |
| Local dev experience | Simple (file-based DuckDB + local ChromaDB) | Simple (Docker mongod + mongot) |

**The SSPL caveat**: MongoDB Community Edition is SSPL, not MIT. For an internal tool or a product you deploy for clients, this is fine. If you're offering MongoDB-as-a-service, it's not. For futuresight-kms, SSPL is not a problem.

---

## Status

Specified 2026-04-01. Research complete. MongoDB 8.2 CE + mongot = the unification.
