# Knowledge Ops — Knoxx Graph Query Contract v1 (FROZEN)

Date: 2026-04-05
Date-Frozen: 2026-04-08
Status: frozen
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 3

## Purpose

Frozen, stable, agent-facing graph query contract. All graph and memory surfaces route through this contract.

## Contract Surface

### 1. `POST /v1/graph/memory` — Unified Memory Tool (PRIMARY)

The canonical agent-facing endpoint. Replaces separate `knowledge_graph` + `semantic_search` + `memory` tools with one call.

**Request:**
```json
{
  "q": "natural language query string",
  "lakes": ["devel", "web", "knoxx-session"],
  "nodeTypes": ["file", "url", "tool_call"],
  "k": 15,
  "maxCost": 2.0,
  "maxNodes": 60,
  "minSimilarity": 0.55,
  "minVectorSimilarity": 0.35,
  "includeText": true
}
```

**Response:**
```json
{
  "query": "...",
  "clusters": [
    { "lake": "devel", "count": 4, "topNodes": [...] },
    { "lake": "web", "count": 26, "topNodes": [...] }
  ],
  "nodes": [{ "id", "score", "traversalCost", "isSeed", "lake", "nodeType", "text" }],
  "edges": [{ "source", "target", "similarity" }],
  "stats": { "vectorHits", "seeds", "visited", "edges", "clusters" }
}
```

**Pipeline:** embed(q) → cosine sim over `graph_node_embeddings` → Dijkstra over `graph_semantic_edges` → lake-cluster → text preview from events collection.

**Defaults:** k=15, maxCost=2.0, maxNodes=60, minSim=0.55, minVectorSim=0.35, includeText=true.

**Lake scoping:** filters both vector candidates AND semantic edges by lake prefix (`devel:`, `web:`, etc.).

### 2. `POST /v1/graph/semantic-search` — Semantic Search + Traversal

Lower-level variant of `/memory`. Same pipeline but without lake clustering or node-type filtering.

**Request:** `{ q, k, maxCost, maxNodes, minSimilarity, minVectorSimilarity }`

**Pipeline:** vector search on `event_chunks` (hot tier) → seed extraction → Dijkstra over `graph_semantic_edges`.

**Use when:** agent needs raw traversal results without clustering.

### 3. `POST /v1/graph/traverse` — Bounded Graph Traversal

Structural traversal from explicit seed node IDs.

**Request:** `{ seedNodeIds[], maxDistance, maxNodes, edgeKinds[], includeSeeds }`

**Pipeline:** BFS/Dijkstra from seeds over `graph_edges` (structural) → fallback to `graph_semantic_edges` if no structural edges found.

**Use when:** agent already knows which nodes to start from.

### 4. `GET /v1/graph/export` — Bulk Graph Export

Full graph dump for visualization or bulk analysis.

**Params:** `projects[]`, `limitNodes`, `limitEdges`, `includeSemantic`, `semanticMinSimilarity`

**Sources:** events collection (nodes), `graph_edges` collection (edges), `graph_semantic_edges` (when includeSemantic=true).

### 5. `GET /v1/graph/stats` — Graph Statistics

Returns `{ ok, nodeCount, edgeCount, storageBackend }`. Read-only health check for graph state.

## Canonical Lakes

| Lake | Node ID Prefix | Description |
|------|---------------|-------------|
| `devel` | `devel:file:` | Workspace files |
| `web` | `web:url:` | Web crawl URLs |
| `bluesky` | `bluesky:` | Bluesky users/posts |
| `knoxx-session` | `knoxx-session:run:` | Session transcripts |

## Canonical Edge Kinds

| Kind | Source | Target | Rest Length | Strength |
|------|--------|--------|-------------|----------|
| `code_dependency` | devel file | devel file | 90 | 0.011 |
| `local_markdown_link` | devel file | devel file | 130 | 0.005 |
| `external_web_link` | any | web url | 220 | 0.0035 |
| `visited_to_visited` | web url | web url | 180 | 0.0024 |
| `visited_to_unvisited` | web url | web url | 210 | 0.0028 |
| `mentions_devel_path` | knoxx-session | devel file | 320 | 0.0015 |
| `mentions_web_url` | knoxx-session | web url | 320 | 0.0015 |
| `semantic_similarity` | any | any | dynamic | dynamic |

## Knoxx Agent Tool Mapping

| Tool Name | OpenPlanner Route | Purpose |
|-----------|-------------------|---------|
| `graph_query` | `/v1/graph/memory` | Primary unified memory tool |
| `semantic_query` | `/v1/graph/similar` | Corpus vector search only |
| `semantic_read` | event read by path | Full document retrieval |
| `memory_search` | `/v1/graph/memory` with lakes=["knoxx-session"] | Session memory search |
| `memory_session` | session load by ID | Transcript drill-down |

## Non-Goals (Explicitly Deferred)

1. Adaptive traversal (ACO/ant-based) — implementation detail behind existing traversal APIs
2. Exposing Graph-Weaver GraphQL directly to agents
3. Redesigning `semantic_query` or `memory_search` as independent tools (they delegate to `/memory`)
4. Real-time streaming results — all queries are bounded request/response

## Versioning Rules

- v1 is frozen. No breaking changes.
- New features go behind new optional params or new endpoints (`/v1/graph/memory/v2`).
- Traversal algorithm changes (Dijkstra → ACO) must preserve the same response shape.
- New lakes or edge kinds are additive only.

## Verification Checklist

- [x] The v1 contract is documented in this single file
- [x] All OpenPlanner routes listed above exist and return documented shapes
- [x] `/v1/graph/memory` tested end-to-end with real data
- [x] Knoxx tool metadata in `agent_hydration.cljs` maps to these routes
- [ ] Knoxx prompt guidelines reference `/v1/graph/memory` as primary tool
- [ ] Future adaptive expansion can land behind same interface
