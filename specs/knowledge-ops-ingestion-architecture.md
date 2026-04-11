# Knowledge Ops Ingestion Architecture

**Status**: Active
**Last Updated**: 2026-04-11
**Context**: Knoxx ingestion replacement and graph-weaver coordination

## Overview

This document clarifies the separation of concerns between Knoxx ingestion, OpenPlanner, and graph-weaver to prevent duplicate work and ensure a clear data flow.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Data Sources                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Knoxx Docs/      │  Chat Sessions   │  External Docs  │  Code Repo │
│  Uploads          │  (conversations) │  (CMS imports)  │  (devel/)  │
└────────┬──────────┴────────┬─────────┴────────┬─────────┴─────┬─────┘
         │                   │                  │               │
         ▼                   ▼                  ▼               │
┌────────────────────────────────────────────────────────────┐  │
│                    OpenPlanner                              │  │
│  ┌──────────────────────────────────────────────────────┐  │  │
│  │  /v1/documents  ← Documents (docs/code/config/data)  │  │  │
│  │  /v1/events      ← Session events, memory, graph     │  │  │
│  │  /v1/search      ← Vector + FTS search               │  │  │
│  │  /v1/graph       ← Graph nodes + edges               │  │  │
│  └──────────────────────────────────────────────────────┘  │  │
│                                                            │  │
│  Storage:                                                  │  │
│  • mongo.events         ← Documents, sessions, graph nodes │  │
│  • mongo.hotVectors     ← Document embeddings              │  │
│  • mongo.graphEdges     ← Structural edges                 │  │
│  • mongo.graphSemantic  ← kNN semantic similarity edges    │  │
│                                                            │  │
└────────────────────────────┬───────────────────────────────┘  │
                             │                                  │
                             ▼                                  │
┌────────────────────────────────────────────────────────────┐  │
│                    Graph-Weaver                             │◄─┘
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Mode: openplanner-graph (RECOMMENDED)               │  │
│  │  • Fetches base graph from OpenPlanner /v1/graph     │  │
│  │  • No duplicate ingestion                            │  │
│  │  • Provides visualization + ACO pathfinding          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Mode: repo (OPTIONAL CODE ANALYSIS)                 │  │
│  │  • Scans local code for import/requires/links        │  │
│  │  • Creates code-level structural edges               │  │
│  │  • Use as overlay, not replacement                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

### OpenPlanner (Source of Truth)

**Primary responsibilities:**
1. **Document Ingestion** (`POST /v1/documents`)
   - Accepts documents with `id`, `title`, `content`, `project`, `kind`
   - Kinds: `docs`, `code`, `config`, `data`
   - Automatically chunks and embeds content
   - Stores in `mongo.events` and `mongo.hotVectors`

2. **Session Memory** (`POST /v1/events`)
   - Receives events from Knoxx chat sessions
   - Stores conversation turns, tool calls, reasoning
   - Builds session graph nodes and edges

3. **Graph Storage** (`GET /v1/graph/export`)
   - Exports all graph nodes and edges
   - Includes structural edges (session flow, tool relationships)
   - Includes semantic edges (kNN similarity from embeddings)

4. **Search** (`POST /v1/search/vector`, `/v1/search/fts`)
   - Vector similarity search over embeddings
   - Full-text search fallback

### Knoxx (Ingestion Client)

**Primary responsibilities:**
1. **Document Ingestion API** (`POST /api/documents/ingest`)
   - Reads files from workspace docs directory
   - Calls OpenPlanner `POST /v1/documents` for each file
   - Tracks ingestion progress locally
   - **NO direct Qdrant access** (removed)

2. **Chat Interface**
   - Uses OpenPlanner `/v1/search` for RAG retrieval
   - Uses OpenPlanner `/v1/events` for session persistence
   - Uses OpenPlanner `/v1/graph` for memory context

### Graph-Weaver (Visualization Layer)

**Primary responsibilities:**
1. **Graph Visualization**
   - Renders nodes and edges in browser
   - Provides ACO pathfinding for navigation
   - Layout algorithms (force-directed, etc.)

2. **Mode Selection** (`GRAPH_WEAVER_LOCAL_SOURCE` env var)
   - `openplanner-graph` (RECOMMENDED): Fetches from OpenPlanner
   - `repo` (CODE ANALYSIS): Scans local files for code-level edges
   - `openplanner-lakes`: Multi-project aggregation
   - `none`: Manual overlay only

## Data Flow

### Document Ingestion Flow

```
1. User uploads document to Knoxx
   ↓
2. Knoxx reads file content
   ↓
3. Knoxx calls OpenPlanner POST /v1/documents
   {
     document: {
       id: "knoxx:default:docs/readme.md",
       title: "readme.md",
       content: "...",
       project: "devel",
       kind: "docs",
       source: "knoxx-ingestion",
       sourcePath: "docs/readme.md"
     }
   }
   ↓
4. OpenPlanner:
   - Stores document in mongo.events
   - Chunks content
   - Generates embeddings
   - Stores in mongo.hotVectors
   ↓
5. Document is searchable via /v1/search
```

### Graph Edge Types

| Edge Type | Source | Kind | Description |
|-----------|--------|------|-------------|
| Session flow | Knoxx/OpenPlanner | `session_flow` | Conversation turns |
| Tool call | Knoxx/OpenPlanner | `tool_call` | Tool invocations |
| Mentions file | Knoxx/OpenPlanner | `mentions_devel_path` | File references in chat |
| Mentions URL | Knoxx/OpenPlanner | `mentions_web_url` | URL references in chat |
| Semantic kNN | OpenPlanner | `semantic_similarity` | Vector similarity |
| **Code import** | Graph-weaver `repo` | `import` | JS/TS imports |
| **Code require** | Graph-weaver `repo` | `dep` | Python/Clojure deps |
| **Markdown link** | Graph-weaver `repo` | `ref`/`link` | Internal/external links |

**Note**: Code-level edges (import, dep, ref, link) are ONLY created by graph-weaver `repo` mode. OpenPlanner does NOT duplicate this analysis.

## Configuration

### Environment Variables

#### OpenPlanner
```bash
OPENPLANNER_BASE_URL=http://openplanner:3000
OPENPLANNER_API_KEY=your-api-key
```

#### Knoxx
```bash
# Inherited from OpenPlanner for ingestion
OPENPLANNER_BASE_URL=http://openplanner:3000
OPENPLANNER_API_KEY=your-api-key
```

#### Graph-Weaver
```bash
# RECOMMENDED: Use OpenPlanner as source
GRAPH_WEAVER_LOCAL_SOURCE=openplanner-graph
OPENPLANNER_BASE_URL=http://openplanner:3000
OPENPLANNER_API_KEY=your-api-key

# OPTIONAL: Add code analysis overlay (not replacement)
# Run graph-weaver with both modes if needed
```

### Migration from Qdrant

**Before (deprecated):**
```clojure
;; Knoxx directly called Qdrant
:start-document-ingestion!
;; → Qdrant upsert (removed)
```

**After:**
```clojure
;; Knoxx calls OpenPlanner
:start-document-ingestion!
;; → op-memory/batch-upsert-openplanner-documents!
;; → OpenPlanner POST /v1/documents
;; → MongoDB + embeddings
```

## Avoiding Duplication

### ❌ DON'T
- Run Knoxx ingestion AND graph-weaver `repo` mode on the same files
- Run multiple graph-weaver instances in `repo` mode
- Store documents in both Qdrant and OpenPlanner

### ✅ DO
- Use OpenPlanner as the single source of truth for documents
- Use graph-weaver `openplanner-graph` mode for visualization
- Use graph-weaver `repo` mode ONLY for code-level analysis overlay
- Treat code-level edges as supplementary, not primary

## Monitoring

### Ingestion Health
```bash
# Check OpenPlanner document count
curl http://localhost:3000/v1/documents?limit=1

# Check graph node count
curl http://localhost:3000/v1/graph/export?limit=1
```

### Knoxx Ingestion Status
```bash
# Check ingestion progress
curl http://localhost/api/documents/ingestion-status
```

## References

- [documents.ts](../../src/routes/v1/documents.ts) - OpenPlanner document API
- [openplanner_memory.cljs](../backend/src/cljs/knoxx/backend/openplanner_memory.cljs) - Knoxx ingestion client
- [document_state.cljs](../backend/src/cljs/knoxx/backend/document_state.cljs) - Knoxx ingestion state
- [openplanner-graph.ts](../../packages/graph-weaver/src/openplanner-graph.ts) - Graph-weaver OpenPlanner integration
- [scan.ts](../../packages/graph-weaver/src/scan.ts) - Graph-weaver code analysis
