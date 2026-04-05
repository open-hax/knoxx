# Knoxx Session Lake + Graph Memory + Active/Passive RAG Alignment

## Status
- state: working spec / implementation note
- date: 2026-04-04
- scope: Knoxx session isolation, session graph projection, graph query tool, OpenPlanner lake cleanup, and ingestion backpressure

## Problem

Knoxx session memory is being cross-contaminated by non-session data because historical Knoxx events and workspace data were sharing broad project/lake boundaries.

At the same time, the system already has the right organs for a better shape:

- `OpenPlanner` as canonical lake
- `Knoxx` as agent/runtime/session producer
- `kms-ingestion` as canonical `devel` producer
- `Myrmex + ShuvCrawl` as canonical `web` producer
- `Graph Weaver` as graph workbench

The missing piece is to give Knoxx its own canonical session lake and project session activity into the graph rather than treating sessions as flat transcript rows only.

## Canonical lakes

- `devel`
- `web`
- `bluesky`
- `knoxx-session`

Legacy split lakes to retire/purge:

- `devel-docs`
- `devel-code`
- `devel-config`
- `devel-data`
- `knoxx-graph`

## Knoxx-session graph model

### Node types

- `user_message`
- `assistant_message`
- `tool_call`
- `tool_result`
- `reasoning`

### Edge types

- `mentions_devel_path`
- `mentions_web_url`

### Target node ID contracts

- devel file node: `devel:file:<workspace-relative-path>`
- web node: `web:url:<normalized-url>`
- knoxx session nodes: `knoxx-session:run:<run-id>:<node-class>`

## Knoxx behavior contract

1. Passive hydration remains useful but incomplete.
2. Transcript/session memory belongs in `knoxx-session`, not `devel`.
3. `memory_search` and `memory_session` must query only `knoxx-session` by default.
4. Knoxx should also have a graph-aware tool for canonical graph lookup.
5. Session messages/tool artifacts should project into graph facts so cross-lake links become queryable.

## Research synthesis

### Passive vs active retrieval

Passive RAG is one-shot retrieval placed into prompt context. It is cheap and often enough for local grounding, but it tends to blur together unrelated records and does not reason over structure very well.

Active/agentic RAG treats retrieval as a control loop:
- search
- inspect
- decide next retrieval
- resolve conflicts/noise
- continue until enough structure is present

This is closer to what Knoxx already wants to be.

### Memory system takeaways

Recent agent-memory work converges on a few stable ideas:

1. **Separate episodic/session memory from semantic/corpus memory**
   - CoALA and related work split semantic, episodic, and procedural memory.
   - For us: `knoxx-session` is episodic memory; `devel/web/bluesky` are semantic/external source lakes.

2. **Graph structure beats flat chunk stores when provenance and relations matter**
   - AriGraph, GAAMA, Zep-style temporal graph memory, and GraphRAG work all point to structured memory as the right layer for long-horizon agents.
   - For us: session nodes linking to `devel` and `web` nodes create the minimal useful graph-memory bridge.

3. **Active retrieval should sit on top of a lake that can absorb raw data first**
   - ActiveRAG and GraphRAG work suggest the lake should ingest broadly and organize later, rather than rejecting data too early.
   - For us: OpenPlanner should accept raw/large session and web data, normalize/chunk for indexing, and preserve canonical raw records.

4. **Backpressure matters as much as retrieval quality**
   - Agentic memory systems become unstable if ingestion outruns embedding/indexing capacity.
   - For us: `kms-ingestion` needs OpenPlanner-aware backoff instead of blindly flooding embeddings.

## System mapping

### Passive layer
- passive semantic hydration from Knoxx corpus
- passive memory hydration from prior `knoxx-session` events

### Active layer
- `semantic_query`
- `semantic_read`
- `memory_search`
- `memory_session`
- `graph_query`

### Canonical graph bridge
- Knoxx emits session graph nodes/edges into `knoxx-session`
- `kms-ingestion` emits canonical `devel` graph
- Myrmex emits canonical `web` graph
- Graph Weaver renders the combined graph

## Implementation phases

### Phase 1
- isolate Knoxx session search to `knoxx-session`
- add OpenPlanner session project filter
- add graph query route/tool
- emit session graph nodes/edges from Knoxx runs

### Phase 2
- add lake purge path for legacy split lakes
- normalize persisted Knoxx config/env to include `KNOXX_SESSION_PROJECT_NAME`
- migrate or purge historical Knoxx-in-`devel` session data

### Phase 3
- add stronger OpenPlanner-aware producer backpressure
- optionally expose graph neighborhood exploration and lake-aware memory dashboards in Knoxx UI

## Verification targets

- Knoxx memory APIs no longer return `devel` corpus rows by default
- New runs write to `project=knoxx-session`
- Session graph nodes appear with requested node types
- Session references to paths/URLs create cross-lake edges
- `graph_query` returns graph nodes/edges to Knoxx agent runtime
- Legacy projects can be purged from OpenPlanner
