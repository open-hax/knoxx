# Knowledge Ops — Source Lakes + Cross-Lake Graph Spec

## Current Canonical Reading

- state: proposed canonical replacement for the split `devel-*` lake model
- canonical lake backend: `orgs/open-hax/openplanner`
- canonical devel producer: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`
- canonical web producer: `orgs/octave-commons/myrmex`
- future canonical Bluesky producer: Sintel / Bluesky firehose ingestion
- primary graph workbench: `orgs/octave-commons/graph-weaver`

This spec supersedes the `devel-docs` / `devel-code` / `devel-config` / `devel-data` split as the **lake boundary model**.
Those values remain useful, but only as **node/edge metadata and query filters**, not as separate lakes.

> *One lake per source. Rich labels inside the lake. One graph spanning the lakes.*

---

## Purpose

Define the canonical graph model so that:

1. `OpenPlanner` is the canonical storage surface.
2. `Knoxx` ingestion, `Myrmex`, and future Bluesky/Sintel ingestion write into the same canonical lake system.
3. `Graph Weaver` is a workbench and visualization surface **over the canonical lake graph**, not a second canonical scanner.
4. lake boundaries are by **data source**, not by content subtype.
5. subtype distinctions (`docs`, `code`, `config`, `data`, etc.) remain first-class through labels, filters, and visualization.

---

## Problem Statement

The current direction has two different issues mixed together:

### 1. Duplicate canonicalization

`Knoxx` ingestion consumes `devel` files.
`Graph Weaver` also consumes `devel` files.
Each can build its own representation and store.
That creates two overlapping models of the same workspace entities, which can drift.

### 2. Wrong lake boundary

The current `devel-docs`, `devel-code`, `devel-config`, and `devel-data` split treats content subtypes as separate lakes.
That makes subtype filters feel like hard storage boundaries when they are actually metadata dimensions.

The canonical model should instead be:

- one `devel` lake
- one `web` lake
- one `bluesky` lake

Then use node/edge metadata to express subtype and relation type.

---

## Goals

1. One lake per source.
2. One canonical graph backed by OpenPlanner.
3. Rich typed nodes and edges inside each lake.
4. Cross-lake edges are explicit and queryable.
5. Graph Weaver shows lake separation clearly.
6. Graph Weaver shows node kinds and edge kinds clearly.
7. Graph Weaver includes a legend that explains the visual encoding.
8. Query filters can recover the equivalent of old split-lake behavior without physically splitting lakes.
9. Bluesky can feed website discovery into the `web` lake.
10. Myrmex can reason over `devel`, `web`, and `bluesky` as one connected graph.

---

## Non-Goals

1. This spec does **not** require Graph Weaver to own canonical ingestion.
2. This spec does **not** require deleting presentation-only Graph Weaver state such as layout overrides, selection state, or user annotations.
3. This spec does **not** require every node kind to be embedded or semantically indexed in the same way.
4. This spec does **not** require lake boundaries to act as access-control boundaries.

---

## Canonical Lake Model

### Lake keys

There is exactly one canonical lake per source family:

| Lake key | Meaning |
|----------|---------|
| `devel` | workspace-derived entities and relations |
| `web` | crawled/discovered web entities and relations |
| `bluesky` | Bluesky/Sintel social entities and relations |

### Canonical mapping into OpenPlanner

For graph records:

| OpenPlanner field | Meaning |
|-------------------|---------|
| `source_ref.project` / `project` | lake key: `devel`, `web`, or `bluesky` |
| `kind` | `graph.node` or `graph.edge` |
| `source` | concrete writer, e.g. `kms-ingestion`, `myrmex`, `sintel`, `graph-weaver` |
| `extra.node_type` | subtype for nodes |
| `extra.edge_type` | subtype for edges |
| `extra.source_lake` | lake of source node |
| `extra.target_lake` | lake of target node |

The lake boundary is therefore carried by `project`, not by node subtype.

---

## Canonical Graph Entity Model

## Node envelope

Every graph node event should carry at least:

```json
{
  "schema": "openplanner.event.v1",
  "kind": "graph.node",
  "source_ref": {
    "project": "devel"
  },
  "extra": {
    "lake": "devel",
    "node_id": "devel:file:orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs",
    "node_type": "code",
    "label": "core.cljs",
    "entity_key": "devel:file:orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs"
  }
}
```

Required node metadata:

| Field | Meaning |
|-------|---------|
| `lake` | `devel` / `web` / `bluesky` |
| `node_id` | stable graph node ID |
| `node_type` | subtype inside the lake |
| `label` | human label |
| `entity_key` | stable cross-record identity for dedupe/linkage |

Optional metadata examples:

| Field | Use |
|-------|-----|
| `path` | devel workspace-relative path |
| `url` | canonical normalized URL |
| `did` | Bluesky user DID |
| `handle` | Bluesky handle |
| `post_uri` | Bluesky post URI |
| `visit_status` | `visited` / `unvisited` |
| `title` | document/page/post title |
| `content_hash` | content dedupe |
| `mime_type` | source media type |

## Edge envelope

Every graph edge event should carry at least:

```json
{
  "schema": "openplanner.event.v1",
  "kind": "graph.edge",
  "source_ref": {
    "project": "devel"
  },
  "extra": {
    "lake": "devel",
    "edge_id": "devel:edge:local_markdown_link:...",
    "edge_type": "local_markdown_link",
    "source_node_id": "devel:file:docs/INDEX.md",
    "target_node_id": "devel:file:specs/service-surface.md",
    "source_lake": "devel",
    "target_lake": "devel"
  }
}
```

Required edge metadata:

| Field | Meaning |
|-------|---------|
| `lake` | provenance lake of the observation/assertion |
| `edge_id` | stable edge ID |
| `edge_type` | relation subtype |
| `source_node_id` | source node |
| `target_node_id` | target node |
| `source_lake` | source node lake |
| `target_lake` | target node lake |

---

## Identity and Deduplication Rules

### Rule 1: stable IDs by source semantics

| Entity | ID shape |
|--------|----------|
| devel file node | `devel:file:<workspace-relative-path>` |
| web page node | `web:url:<normalized-url>` |
| bluesky user node | `bluesky:user:<did-or-handle>` |
| bluesky post node | `bluesky:post:<at-uri>` |

### Rule 2: graph node vs content document records may coexist, but must share identity hooks

A devel file may produce:
- a content/retrieval record (`docs`, `code`, `config`, `data`)
- a graph node record (`graph.node`)
- zero or more graph edge records (`graph.edge`)

That is acceptable **only if** they share stable linkage metadata such as:
- `extra.entity_key`
- `extra.path`
- `extra.url`
- `extra.did`
- etc.

The problem is not “more than one event kind exists.”
The problem is “two different systems own the same canonical entity model and can diverge.”

### Rule 3: Graph Weaver must not be a second canonical producer for devel graph facts

For `devel` canonical graph facts, Graph Weaver should read from OpenPlanner, not derive a competing truth by separately scanning `devel` as the authoritative source.

---

## Lake-Scoped Node Types

## `devel` node types

| Node type | Meaning |
|-----------|---------|
| `docs` | markdown, notes, specs, READMEs, narrative docs |
| `code` | source code and tests |
| `config` | configuration and infra-shaped text artifacts |
| `data` | datasets, reports, exports, structured artifacts |

These are metadata labels and query filters inside the `devel` lake.
They are not separate lakes.

## `web` node types

| Node type | Meaning |
|-----------|---------|
| `visited` | discovered and successfully fetched/processed URL |
| `unvisited` | discovered URL still in frontier / not yet fetched |

## `bluesky` node types

| Node type | Meaning |
|-----------|---------|
| `user` | Bluesky account / actor |
| `post` | Bluesky post / skeet |

---

## Lake-Scoped Edge Types

## `devel` edge types

| Edge type | Source -> Target | Meaning |
|-----------|------------------|---------|
| `local_markdown_link` | `docs -> any devel node` | markdown/reference link inside devel corpus |
| `external_web_link` | `devel -> web` | external URL discovered from devel corpus |
| `code_dependency` | `devel code -> devel code` | import/require/dependency relation inside devel |

## `web` edge types

| Edge type | Source -> Target | Meaning |
|-----------|------------------|---------|
| `visited_to_visited` | `visited -> visited` | crawled page linked to another already-visited page |
| `visited_to_unvisited` | `visited -> unvisited` | crawled page discovered a frontier URL |

## `bluesky` edge types

| Edge type | Source -> Target | Meaning |
|-----------|------------------|---------|
| `follows_user` | `user -> user` | social follow relation |
| `authored_post` | `user -> post` | authorship relation |
| `shared_post` | `user -> post` | repost/share relation |
| `liked_post` | `user -> post` | like relation |
| `post_links_visited_web` | `post -> visited web` | URL in post that has already been crawled |
| `post_links_unvisited_web` | `post -> unvisited web` | URL in post that should feed crawl frontier |

---

## Producer Responsibilities

## 1. Knoxx ingestion (`devel` producer)

The Knoxx ingestion engine is the canonical producer for `devel` graph facts.

Responsibilities:
- classify files as `docs`, `code`, `config`, or `data`
- write content/retrieval records into the `devel` lake
- write `graph.node` records for devel entities into the `devel` lake
- write `local_markdown_link`, `external_web_link`, and `code_dependency` edges into the `devel` lake
- preserve stable IDs and `entity_key`

## 2. Myrmex (`web` producer)

Myrmex is the canonical producer for `web` graph facts.

Responsibilities:
- create `visited` and `unvisited` web nodes in the `web` lake
- write `visited_to_visited` and `visited_to_unvisited` edges
- consume `external_web_link` seeds from `devel`
- later consume `post_links_unvisited_web` seeds from `bluesky`

## 3. Sintel / Bluesky ingestion (`bluesky` producer)

Sintel or equivalent Bluesky ingestion becomes the canonical producer for `bluesky` graph facts.

Responsibilities:
- create `user` and `post` nodes in the `bluesky` lake
- write `follows_user`, `authored_post`, `shared_post`, `liked_post` edges
- extract URLs from posts
- create `post_links_visited_web` / `post_links_unvisited_web` edges
- surface crawl candidates for Myrmex

---

## Cross-Lake Relationship Rules

Cross-lake edges are first-class.
They are not an afterthought.

### Canonical allowed cross-lake relations

| Provenance lake | Edge type | Source lake -> Target lake |
|-----------------|-----------|-----------------------------|
| `devel` | `external_web_link` | `devel -> web` |
| `bluesky` | `post_links_visited_web` | `bluesky -> web` |
| `bluesky` | `post_links_unvisited_web` | `bluesky -> web` |

Later cross-lake relations may be added explicitly, but should not be inferred silently.

---

## Query and Filter Contract

The old split-lake behavior must be recoverable by query.

Examples:

### Equivalent of old `devel-code`

```json
{
  "project": "devel",
  "kind": "graph.node",
  "extra.node_type": "code"
}
```

### Equivalent of old `devel-docs`

```json
{
  "project": "devel",
  "kind": "graph.node",
  "extra.node_type": "docs"
}
```

### Cross-lake discovery from Bluesky into crawl frontier

```json
{
  "projects": ["bluesky", "web"],
  "edge_type": "post_links_unvisited_web"
}
```

### All devel-origin external website references

```json
{
  "project": "devel",
  "kind": "graph.edge",
  "extra.edge_type": "external_web_link"
}
```

Therefore subtype filters replace the need for physical subtype lakes.

---

## Graph Weaver Contract

Graph Weaver is the graph workbench over the canonical lake graph.

### It must do

1. Read canonical graph nodes/edges from OpenPlanner.
2. Preserve separate user-layer layout/annotation state if desired.
3. Show lake membership clearly.
4. Show node type clearly.
5. Show edge type clearly.
6. Support filtering by:
   - lake
   - node type
   - edge type
   - source writer
7. Support meta-queries equivalent to old split-lake views.
8. Show cross-lake relationships as explicit, visually distinct connections.

### It must not do

1. Be the authoritative scanner for `devel` graph truth.
2. Replace canonical lake data with an alternate local-only graph model.
3. Hide provenance or blur lake membership.

---

## Graph Weaver Visualization Contract

### 1. Lake separation must be visually obvious

Graph Weaver should visually separate `devel`, `web`, and `bluesky` using one or more of:
- swimlanes
- background hulls / cluster regions
- grouped columns
- lake-level boundary outlines

This is a hard UI requirement, not a nice-to-have.

### 2. Node kinds must be visually separable

Recommended encoding:
- primary grouping/color family by `lake`
- secondary encoding by `node_type` via shade, border, icon, or shape

Example:
- `devel` = one color family
- `web` = another color family
- `bluesky` = another color family
- within each family, `docs`/`code`/`config`/`data` or `visited`/`unvisited` or `user`/`post` get distinct shape/icon/border treatments

### 3. Edge kinds must be visually separable

Recommended encoding:
- color by `edge_type`
- line style by cross-lake vs intra-lake
- optional arrowhead variants by relation family

### 4. Legend is required

Graph Weaver must include a graph legend that explains:
- lake color families
- node type encodings
- edge type encodings
- cross-lake edge styling

The legend should be visible from the graph UI without requiring source inspection.

### 5. Filter UI is required

Graph Weaver should expose checkboxes or toggles for:
- lake
- node type
- edge type
- cross-lake only / intra-lake only

---

## Knoxx Agent Contract

Knoxx should be able to query this graph model through canonical filters, not through hardcoded split lakes.

The agent should be able to ask questions like:
- show me only `devel` `code` nodes
- show all `devel -> web` edges
- show Bluesky posts linking to unvisited web nodes
- show the subgraph around one workspace file across all related lakes

This means the graph query surface must expose:
- lake
- node type
- edge type
- stable IDs
- cross-lake source/target lake metadata

---

## Migration Plan

## Phase 1 — Canonical lake correction

- replace `devel-docs`, `devel-code`, `devel-config`, `devel-data` with one `devel` lake for graph records
- preserve subtype as node metadata: `node_type`
- keep query compatibility through filters and views

## Phase 2 — Canonical devel graph ingestion

- extend Knoxx ingestion to emit canonical `graph.node` and `graph.edge` events for devel
- ensure stable IDs and shared `entity_key`
- stop treating Graph Weaver repo scan as canonical truth for devel graph facts

## Phase 3 — Canonical web graph ingestion

- keep Myrmex as producer for `web`
- make sure web nodes are typed as `visited` / `unvisited`
- preserve devel-origin external links as `devel -> web` cross-lake edges

## Phase 4 — Canonical Bluesky graph ingestion

- ingest Bluesky/Sintel into `bluesky`
- create `user` and `post` nodes and edge taxonomy from this spec
- feed post URLs into web frontier

## Phase 5 — Graph Weaver visualization upgrade

- make Graph Weaver read canonical graph facts from OpenPlanner
- add lake boundaries
- add node/edge type encodings
- add required legend and filter controls

---

## Verification Plan

### Data model verification

- no new graph events for devel use `project=devel-docs|devel-code|devel-config|devel-data`
- devel graph events use `project=devel`
- node subtype appears in `extra.node_type`
- edge subtype appears in `extra.edge_type`

### Producer verification

- Knoxx devel producer emits devel nodes and edges
- Myrmex emits web nodes and edges
- Bluesky/Sintel emits bluesky nodes and edges
- cross-lake edges preserve `source_lake` and `target_lake`

### Graph Weaver verification

- graph renders all three lakes distinctly
- legend is visible and correct
- `.clj-kondo` and similar noise appear only if canonical devel producer explicitly writes them
- filtering `lake=devel, node_type=code` reproduces the old conceptual `devel-code` view

---

## Existing Code References

| File | Role |
|------|------|
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj` | current devel ingestion/classification |
| `orgs/open-hax/openplanner/src/routes/v1/events.ts` | canonical event ingest surface |
| `orgs/open-hax/openplanner/src/routes/v1/search.ts` | query/filter surface |
| `orgs/open-hax/openplanner/src/routes/v1/graph.ts` | current graph-oriented API surface |
| `orgs/octave-commons/myrmex/src/graph-store.ts` | current web graph writer |
| `orgs/octave-commons/graph-weaver/src/server.ts` | current graph workbench |
| `orgs/open-hax/knoxx/specs/knowledge-ops-role-scoped-lakes.md` | superseded split-lake conceptual model |
| `orgs/open-hax/knoxx/specs/knowledge-ops-federated-lakes.md` | superseded split-lake federation language for `devel-*` |

---

## Status

Specified 2026-04-04 from clarified operator intent:
- one lake per data source
- rich node/edge typing within each lake
- one canonical cross-lake graph
- Graph Weaver as a view/workbench over the canonical lake graph
- required visual lake separation and legend
