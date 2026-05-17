# OpenPlanner Hot Query Cache and Session Projection Spec

Status: draft spec 2026-05-15
Scope: Knoxx reads of OpenPlanner sessions/searches, Redis cache/projection layer, short-lived semantic query-result cache

## 1. Recovered intent

OpenPlanner is the durable memory/event/vector system, but it is also under continuous ingest pressure from Discord and other event agents. Knoxx currently asks OpenPlanner expensive questions repeatedly from the frontend and backend. The most visible pain is session listing, but the deeper issue is that hot UI/runtime queries are being served from cold canonical stores.

The desired shape:

1. OpenPlanner remains canonical for durable events, embeddings, graph state, and long-term search.
2. Redis becomes the hot projection/read path for session lists, active summaries, and small materialized indexes.
3. Knoxx reads Redis projections first and only falls back to OpenPlanner when projections are missing/stale.
4. Expensive semantic searches get a short-lived query-result cache, with exact-query hits first and optional fuzzy-query hits through a small query-embedding collection.
5. Cache entries are always scoped by tenant/project/actor permissions/search params and invalidated by corpus watermarks, not by vibes.

This is complementary to the Socket.IO actor bus spec. The actor bus reduces repeated requests; the cache makes the remaining requests cheap.

## 2. Current evidence

### 2.1 Knoxx Redis usage today

From `backend/src/cljs/knoxx/backend/session_store.cljs` and related code:

- Active Knoxx sessions are written to Redis under `knoxx:session:*` with TTL.
- Conversation-to-active-session mapping is written under `knoxx:conversation_to_session:*`.
- Active session ids are tracked in `knoxx:active_sessions`.
- There is an in-process `session-cache*` for active sessions.
- Completed session Redis cache entries are evicted shortly after completion unless sticky.
- Session titles are cached in memory and Redis under `knoxx:session-title:*` for 7 days.
- Run events are available from Redis lists via `run-state/run-events-key`.

What is not present: a Redis-backed archived session list/index projection for `/api/memory/sessions`.

### 2.2 Knoxx `/api/memory/sessions` today

From `backend/src/cljs/knoxx/backend/routes/memory.cljs`:

- Knoxx calls OpenPlanner `/v1/sessions?project=...`.
- It then authorizes session ids.
- Actor/exclusion filters often fetch full session rows per page to inspect actor claims.
- It enriches with Redis active-session state afterward.

This means a small-looking request can still cause expensive OpenPlanner aggregation plus per-session row fetches.

### 2.3 OpenPlanner `/v1/sessions` today

From `/home/err/devel/orgs/open-hax/openplanner/src/routes/v1/sessions.ts`:

- `/v1/sessions` runs a Mongo aggregation over `events`:
  - `$match` project/session
  - `$group` by project/session
  - `$max` timestamp
  - `$sum` event count
  - `$sort` by `last_ts`
  - `$skip`/`$limit`
  - separate aggregation for total count
- This is correct as a cold canonical query, but bad as a hot UI poll path.

### 2.4 Existing OpenPlanner caching/tiering

Relevant existing pieces:

- `src/lib/source-hydration.ts` has a layered hydration cache: memory LRU, optional Redis (`OPENPLANNER_HYDRATION_REDIS_URL`), optional LMDB.
- `src/lib/embedding-runtime.ts` creates hot/compact embedding runtime layers.
- `src/lib/embedding-cache.ts` currently behaves mostly as an in-memory LRU via `getMany`/`putMany`; `set`, `has`, `delete`, and `flush` are stubs.
- `specs/2026-03-27-dual-tier-semantic-memory.md` defines hot/raw and compacted semantic stores.
- `src/lib/semantic-compaction.ts` exists, but the immediate session-list problem is projection caching, not semantic compaction.

## 3. Cache hierarchy

Use four tiers, each with a different job.

| Tier | Store | Purpose | TTL / invalidation |
|---|---|---|---|
| L0 | Browser memory via realtime store | Avoid duplicate component requests in one tab | subscription lifetime |
| L1 | Knoxx process memory | Request coalescing and tiny stale-while-revalidate windows | 1–10s |
| L2 | Redis | Shared hot projections and exact result cache | seconds to hours, invalidated by watermarks |
| L3 | Mongo canonical / vector collections | Durable truth and cold rebuild path | durable / collection TTLs |

Rule: Redis should store **projection summaries and result references**, not giant raw embedding arrays.

## 4. First win: Redis session index projection

### 4.1 Goal

Stop asking Mongo to regroup all events for every session list request. OpenPlanner should maintain session summaries incrementally as events are written. Knoxx should read those summaries from Redis.

### 4.2 Redis keys

Use OpenPlanner-owned keys with an `op:` prefix so they do not collide with Knoxx runtime keys.

```text
op:{tenant}:sessions:z                 # ZSET session_id -> last_ts_ms
op:{tenant}:session:{session_id}:json   # JSON session summary
op:{tenant}:project:{project}:sessions:z
op:{tenant}:actor:{actor_id}:sessions:z
op:{tenant}:contract:{contract_id}:sessions:z
op:{tenant}:session:{session_id}:actors # SET actor claims, optional
op:{tenant}:session:{session_id}:contracts # SET contract claims, optional
op:{tenant}:sessions:watermark          # latest event ts/id/count used by projection
```

Session summary shape:

```ts
type CachedSessionSummary = {
  project: string;
  session: string;
  last_ts: string;
  last_ts_ms: number;
  event_count: number;
  actor_id?: string;
  contract_id?: string;
  contract_actors?: string[];
  title?: string;
  title_model?: string;
  latest_user_message?: string;
  updated_at: string;
  projection_version: "session-index-v1";
};
```

### 4.3 Write path

When OpenPlanner ingests an event:

1. Normalize tenant/project/session.
2. If session id exists, update `op:{tenant}:session:{id}:json`:
   - increment event count;
   - update `last_ts` if newer;
   - extract actor/contract claims from `extra` when present;
   - update title/latest user message when event kind warrants it.
3. Update relevant sorted sets by timestamp score.
4. Bump `op:{tenant}:sessions:watermark`.
5. Publish `session.index.patch` on actor bus / Socket.IO when enabled.

### 4.4 Read path in Knoxx

`GET /api/memory/sessions` should become:

1. Normalize `tenant`, `project`, `actorId`, `contractId`, `excludeActorIds`, `limit`, `offset`.
2. Choose the narrowest Redis sorted set:
   - contract set if `contractId` exists;
   - actor set if `actorId` exists;
   - project set if only project exists;
   - all sessions as fallback.
3. `ZREVRANGE` ids with a small overfetch window.
4. Batch `GET` session summaries.
5. Apply auth and exclude filters using cached actor/contract claims.
6. Overlay Knoxx live Redis session state (`knoxx:session:*`) for `is_active`, `active_status`, `has_active_stream`.
7. Return.
8. On Redis miss/stale projection, fall back to OpenPlanner Mongo query and asynchronously rebuild the projection.

### 4.5 TTL / invalidation

Session projection is not purely TTL-based. It is event-updated. Still add TTLs for self-healing:

- summaries: 7 days sliding TTL;
- sorted sets: no TTL or daily refresh;
- per-query response cache: 5–30s TTL for compatibility endpoints;
- stale-while-revalidate: allow serving up to 60s stale if OpenPlanner is busy, but mark response `cache_stale: true`.

## 5. Exact result cache for expensive OpenPlanner endpoints

Before fuzzy semantic caching, implement deterministic exact cache.

### 5.1 Candidate endpoints

| Endpoint | Cache value | TTL | Invalidation |
|---|---|---:|---|
| `/v1/sessions` | list page summaries | 5–30s | sessions watermark |
| `/v1/sessions/:id?mode=visibility` | projected visibility/actor claims | 5–30m | session watermark |
| `/v1/sessions/:id?mode=resume` | message rows | 1–10m | session watermark |
| `/v1/search` / memory search equivalents | result refs + snippets | 1–30m | corpus watermark |
| graph embedding coverage/dashboard | summary object | 15–60s | graph/embedding watermark |

### 5.2 Cache key

```text
op:qcache:exact:{tenant}:{endpoint}:{params_hash}:{corpus_watermark}:{sha256(normalized_query_or_empty)}
```

For non-query list endpoints, `normalized_query_or_empty = ""`.

Parameters must include permission-significant filters:

- tenant/org/project;
- actor id / allowed actor scope;
- contract id;
- visibility;
- source/lake;
- limit/offset/k;
- search mode;
- embedding model/dimensions;
- quality filters.

Never cache across permission boundaries.

## 6. Fuzzy query-result cache

### 6.1 What it is

A short-lived cache of previous expensive semantic query results. It does not replace canonical vector search. It says: “this new query is close enough to a recently served query with identical permissions and search params; serve that result immediately, optionally revalidate in the background.”

### 6.2 Collection

Mongo collection: `query_result_cache`

```ts
type QueryResultCacheEntry = {
  _id: string;
  tenant_id: string;
  project?: string;
  endpoint: string;
  params_hash: string;
  permissions_hash: string;
  normalized_query: string;
  query_hash: string;

  cache_embedding_model: string;       // e.g. nomic-embed-text or qwen3 prefix model
  cache_embedding_dimensions: number;  // 64, 128, etc.
  query_embedding_small: number[];

  result_source_model: string;         // full search model, e.g. qwen3-embedding
  result_source_dimensions: number;    // canonical result vector dims
  corpus_watermark: string;

  results: Array<{
    id: string;
    session?: string;
    node_id?: string;
    score?: number;
    distance?: number;
    snippet?: string;
    source_ref?: Record<string, unknown>;
  }>;

  created_at: Date;
  expires_at: Date;
  last_hit_at?: Date;
  hit_count: number;
  stale_while_revalidate_until?: Date;
};
```

Indexes:

```ts
query_result_cache.createIndex({ expires_at: 1 }, { expireAfterSeconds: 0 });
query_result_cache.createIndex({ tenant_id: 1, endpoint: 1, params_hash: 1, permissions_hash: 1, corpus_watermark: 1 });
// Atlas/local vector search index over query_embedding_small.
```

### 6.3 Request flow

1. Normalize query text.
2. Exact cache check by `query_hash` and params.
3. If exact miss, compute **cheap cache embedding** only.
4. Vector search `query_result_cache.query_embedding_small` with filters:
   - tenant;
   - endpoint;
   - params hash;
   - permissions hash;
   - compatible corpus watermark or accepted stale window.
5. If top similarity >= threshold, return cached results with:
   - `cache_hit: "semantic"`;
   - `matched_query`;
   - `cache_similarity`;
   - `stale: boolean`.
6. If miss, execute canonical full search, then write exact + fuzzy cache entry.

### 6.4 Similarity thresholds

Start conservative:

| Query kind | Threshold | Notes |
|---|---:|---|
| Session/memory search | 0.94+ cosine | Avoid returning semantically adjacent but wrong memories. |
| Exploratory graph search | 0.90+ | User intent can tolerate broader match. |
| Contract/code search | 0.96+ | Precision matters. |
| Exact normalized query | 1.0 hash | No embedding needed. |

### 6.5 Nomic vs MRL vs Qwen prefix

Use the cache embedding only to compare **query to prior query**, not query to corpus. The cached results still come from the full canonical search.

Options:

1. **Nomic cache-key embeddings**
   - Pros: likely cheaper/faster than Qwen; can run as separate “cache key” model.
   - Cons: another model to operate; semantic neighborhoods may differ from full Qwen search.
   - Good default if embedding generation is part of the bottleneck.

2. **MRL / prefix dimensions from Qwen3 embeddings**
   - Pros: same semantic space as canonical search if model supports Matryoshka representation or configurable output dimensions.
   - Cons: if the API only returns full vectors, generating a 1024-ish vector just to check cache defeats part of the purpose.
   - Good if Qwen endpoint can return 64/128 dimensions directly, or if the full query embedding has already been generated.

3. **Random projection / PCA of full embeddings**
   - Pros: can compress stored query embeddings.
   - Cons: does not avoid generating the full incoming query embedding; adds drift/risk.
   - Not first choice.

Recommendation: implement exact Redis cache + session projection first. Then implement fuzzy query cache with `nomic-embed-text` or direct low-dimensional Qwen output if supported. Do not block session caching on MRL.

## 7. Relationship to semantic compaction

Semantic compaction and query-result caching solve different problems.

| Mechanism | Solves | Does not solve |
|---|---|---|
| Session projection cache | Hot UI list/status queries | Semantic recall quality |
| Exact result cache | Repeated identical expensive searches | Similar query reuse |
| Fuzzy query-result cache | Similar repeated searches | Canonical freshness without watermarks |
| Semantic compaction | Retrieval quality over noisy short messages | Hot endpoint fanout/polling |
| Semantic force/cache layer | Graph traversal/layout/runtime fields | Basic session list slowness |

Compaction can remain a later retrieval-quality upgrade. The first production win is session projection + exact query cache.

## 8. OpenPlanner/Knoxx ownership

### OpenPlanner owns

- canonical events and vector documents;
- session projection writes;
- corpus/session watermarks;
- query result cache collection;
- invalidation semantics;
- optional Redis and Mongo cache materialization.

### Knoxx owns

- reading session projections for UI/runtime;
- overlaying active local session state from Knoxx Redis;
- actor/contract authorization checks;
- request coalescing and L1 stale-while-revalidate;
- Socket.IO subscription fanout to frontend.

### Shared

- Redis connection config and key naming contract;
- schema version labels;
- cache telemetry fields in responses.

## 9. Telemetry

Every cached endpoint should expose debug fields in non-production or behind a flag:

```json
{
  "cache": {
    "hit": true,
    "tier": "redis-session-index|exact|semantic|memory",
    "stale": false,
    "ageMs": 1234,
    "watermark": "...",
    "keyHash": "..."
  }
}
```

Metrics:

- cache hit/miss by endpoint;
- OpenPlanner fallback count;
- rebuild count;
- semantic cache false-positive reports if user opens manual refresh;
- p50/p95 latency with and without cache;
- Mongo aggregation count for `/v1/sessions`.

## 10. Phased plan

### Phase 0 — measure and stop duplicate callers

- Keep the frontend polling reduction/socket spec active.
- Add request logging counters for `/api/memory/sessions` and OpenPlanner `/v1/sessions`.
- Add a dev warning when the same selector is requested repeatedly within a short window.

### Phase 1 — Knoxx-side session response cache

Fastest local mitigation:

- Add a small Redis/in-process cache around Knoxx `/api/memory/sessions` keyed by normalized params and auth scope.
- TTL 5–15s.
- Coalesce concurrent identical requests.
- Overlay live session state after reading cached archived rows.

This reduces blast radius immediately but does not remove OpenPlanner aggregation long term.

### Phase 2 — OpenPlanner Redis session projection

- Implement OpenPlanner write-through session summary projection on event ingest.
- Add backfill job to rebuild Redis projection from Mongo events.
- Add OpenPlanner `/v1/sessions` fast path from Redis with Mongo fallback.
- Expose projection watermark/status.

### Phase 3 — Knoxx reads projection directly

- Let Knoxx call OpenPlanner fast path or read shared Redis keys directly by contract.
- Add server-side actor/contract filters.
- Remove per-session row fetches from common list path.

### Phase 4 — exact result cache

- Add exact query/list cache keyed by params + permissions + watermark.
- Apply to memory search and dashboard-ish endpoints.

### Phase 5 — fuzzy query-result cache

- Add `query_result_cache` collection with TTL and small embedding index.
- Start with conservative thresholds and telemetry.
- Return cached results only when search params and permission scope match.
- Optional stale-while-revalidate when OpenPlanner is busy.

### Phase 6 — compaction integration

- Revisit semantic compaction for recall quality after hot query pressure is down.
- Decide whether compact store should feed canonical search results before or after cache layer.

## 11. Acceptance criteria

1. `/api/memory/sessions` no longer triggers Mongo-wide aggregation on every request.
2. Selected-agent sessions are served from actor/contract-indexed projections.
3. Multiple identical frontend requests coalesce to one backend read and then disappear after Socket.IO migration.
4. Redis cache entries are scoped by tenant/project/auth/search params.
5. Query result cache never crosses permission boundaries.
6. Fuzzy cache reports `matched_query` and `cache_similarity` for auditability.
7. Any stale result is marked stale and revalidated asynchronously.
8. No raw large embedding matrices are pushed through Redis.

## 12. Short answer

Yes: cache sessions first. Do it as a Redis projection, not just a blind response cache. Then add exact result caching. Only after that add the low-dimensional semantic query-result cache. Nomic or MRL can be useful there, but they should be a cache-key layer for previous queries, not a replacement for canonical Qwen/OpenPlanner search.
