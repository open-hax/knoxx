---
uuid: "knoxx-knowledge-ops-clojure-backend-migration"
title: "Knowledge Ops — Clojure Backend Migration"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.382Z"
source: "specs/epics/knowledge-ops-clojure-backend-migration.md"
points: null
category: "epics"
---

# Knowledge Ops — Clojure Backend Migration

> Source: `specs/epics/knowledge-ops-clojure-backend-migration.md`

## Current Canonical Reading

- state: current migration spec with partially landed implementation
- canonical backend target today: `orgs/open-hax/knoxx/backend` (shadow-cljs + Fastify)
- canonical ingestion/query donor now lives at: `orgs/open-hax/knoxx/ingestion`
- `ragussy` references remain useful as donor context only

> *Keep the frontend. Replace the spine.*

---
## Purpose

Define the migration from the current Python/Ragussy-centered backend shape to a Clojure/OpenPlanner-centered backend shape.

Triage 2026-05-29: Well-specified architecture epic grounded in live code. Phase 1 (kms-ingestion Clojure service) is already partially landed per the spec. Immediate next step is clear: build kms-query in Clojure with role-preset/lake-selection endpoints before rewriting the frontend. No external blockers — OpenPlanner, Proxx, and the canonical backend target are all present in the repo. The four-phase plan, DoD references, and OpenPlanner contract are well defined. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
