# Knowledge Ops — Gardens Spec

> *A garden is a named operator surface over the same runtime. Not every useful interface is a chat.*

---

## Purpose

Define **gardens** as first-class workbench views in the OpenPlanner-first architecture.

A garden is:
- a domain-specific projection over one or more lakes
- a specialized operator UI
- a reusable microservice surface that may later interest clients
- but first, a tool useful to the workspace owner

---

## Definition

The product is not just:
- ingestion
- CMS
- chat
- labeling

It is also the set of **gardens** that let the operator work in focused, non-chat-native modes.

Examples:
- dependency review
- truth resolution
- exposure review
- graph views
- lake-specific dashboards

---

## Existing Gardens

### 1. Dependency Garden

Existing implementation:
- `services/devel-deps-garden/`

Purpose:
- visualize internal dependencies across the workspace
- find isolated or disconnected projects
- review architectural sprawl

Current contract:
- generates dependency graphs from internal `package.json` relationships
- serves graph reports over HTTP

This is not “just a report.” It is a garden over the workspace graph.

### 2. Truth Workbench

Existing implementation:
- `services/eta-mu-truth-workbench/`

Purpose:
- operator truth-resolution surface
- low-level truth APIs
- control-plane state for known vaults
- receipts and actuation log review

This is a truth garden over runtime state.

### 3. Query Garden

Existing implementation:
- current `/api/query/*` surface in the Clojure service

Purpose:
- federated lake search
- cross-lake synthesis
- role-preset exploration

### 4. Ingestion Garden

Existing implementation:
- current `/api/ingestion/*` surface in the Clojure service

Purpose:
- file/system intake
- source routing into lakes
- background ingest monitoring

---

## Garden Model

Each garden should declare:

| Field | Meaning |
|------|---------|
| `garden_id` | stable identifier |
| `title` | human-facing title |
| `purpose` | what work it supports |
| `lakes` | default lake set |
| `views` | graph, table, list, canvas, timeline, chat, etc. |
| `actions` | what the operator can do |
| `outputs` | what artifacts it produces |

### Example

```json
{
  "garden_id": "devel-deps-garden",
  "title": "Dependency Garden",
  "purpose": "Review workspace dependency topology",
  "lakes": ["devel-code", "devel-config"],
  "views": ["graph", "table"],
  "actions": ["focus-node", "trace-dependency", "find-isolates"],
  "outputs": ["dependency-report", "isolates-list"]
}
```

---

## Why Gardens Matter

If everything is forced into chat, the product becomes shallow.

Gardens preserve:
- task-specific workflows
- non-conversational reasoning surfaces
- domain-specific affordances
- reusable operator tools

Clients may want some of them later.
But they are worth building now because they are already useful in `devel`.

---

## OpenPlanner Relationship

OpenPlanner is the runtime.
Gardens are views over OpenPlanner and adjacent operator data.

That means:
- one canonical store
- many operator surfaces
- no need to build each tool as a separate isolated backend

Some gardens may combine:
- OpenPlanner search
- Cephalon reasoning
- Proxx synthesis
- local graph or truth APIs

But the store remains shared.

---

## Existing Code References

| File | Role |
|------|------|
| `services/devel-deps-garden/README.md` | dependency graph garden |
| `services/eta-mu-truth-workbench/README.md` | truth-resolution garden |
| `orgs/open-hax/knoxx/specs/knowledge-ops-workbench-ui.md` | unified workbench shell |
| `orgs/open-hax/knoxx/specs/knowledge-ops-architecture-migration.md` | OpenPlanner-first migration |

---

## Next Step

The workbench should expose a **Gardens** switcher alongside:
- Chat
- Query
- CMS
- Ingestion
- Labels
- Synthesize

And the first gardens to integrate are:
1. Dependency Garden
2. Truth Workbench

---

## Status

Specified 2026-04-02. Derived from existing devel operator surfaces.
