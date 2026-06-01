---
uuid: "knoxx-knowledge-ops-cms-data-model"
title: "Knowledge Ops — CMS Data Model Spec"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.383Z"
source: "specs/epics/knowledge-ops-cms-data-model.md"
points: null
category: "epics"
---

# Knowledge Ops — CMS Data Model Spec

> Source: `specs/epics/knowledge-ops-cms-data-model.md`

> *The document is the atom. Visibility is the gate. Publish is the sync.*

---
## Purpose

Define the data model, state machine, and API contract for the agent-aware CMS layer that controls the boundary between internal knowledge and public-facing content.

## Canonical Status

This spec originally modeled the CMS as a separate Postgres document store that syncs into a vector store.

That model is now considered **transitional and architecturally broken**.

### Broken assumption

Triage 2026-05-29: Epic defines the agent-aware CMS data model, state machine, and API contract governing the boundary between internal knowledge and public-facing content. The kanban task file is truncated (31 lines, ends mid-section at "### Broken assumption") and the source spec (specs/epics/knowledge-ops-cms-data-model.md) is not present in the current checkout — the prior model (separate Postgres doc store syncing to vector store) is explicitly flagged as "transitional and architecturally broken." Three active CMS epics (folder-backed-visual-cms-design-spec, broadcast-studio-playlist-publication-and-block-cms, knowledge-ops-workbench-ux-v1) directly cite this spec as a dependency. Clear purpose, relevant to knoxx/openplanner, not externally blocked — the work needed is to author the replacement data model spec and recover/complete the truncated kanban body. Next step: move to breakdown, write the updated CMS data model spec (document type, visibility gate, publish state machine, API contract), then split into child tasks ≤5sp each. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
