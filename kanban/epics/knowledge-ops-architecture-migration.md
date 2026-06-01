---
uuid: "knoxx-knowledge-ops-architecture-migration"
title: "Knowledge Ops — Architecture Audit & Migration Spec"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.381Z"
source: "specs/epics/knowledge-ops-architecture-migration.md"
points: null
category: "epics"
---

# Knowledge Ops — Architecture Audit & Migration Spec

> Source: `specs/epics/knowledge-ops-architecture-migration.md`

## Current Canonical Reading

- state: mixed-era migration spec, partially superseded
- canonical product/runtime today: **Knoxx**
- live Knoxx source roots:
  - `orgs/open-hax/knoxx/backend`
  - `orgs/open-hax/knoxx/frontend`
  - `orgs/open-hax/knoxx/ingestion`
  - `services/knoxx`
- read references to `ragussy` and `futuresight-kms` here as historical donor or migration context unless a line explicitly calls out the current Knoxx implementation

> *Kill Ragussy. Promote OpenPlanner. Make services thin. Package the runtime.*

---
Triage 2026-05-29: Well-specified architecture epic with clear problem statement (source code living in services directories), actionable 5-phase roadmap, explicit files-to-create/kill/migrate tables, and no external blockers. Directly relevant to Knoxx/OpenPlanner knowledge-graph project. Spec was authored 2026-04-02 and is internally coherent. No story points assigned — needs breakdown into sub-tasks before work begins (OpenPlanner document endpoints, Cephalon Proxx client, service restructure, UI migration phases each warrant their own tasks). P2 is appropriate: this is important structural work that unblocks the unified runtime vision but is not a production incident. Verdict: accepted (P2). --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
