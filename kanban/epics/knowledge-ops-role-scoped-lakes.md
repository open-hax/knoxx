---
uuid: "knoxx-knowledge-ops-role-scoped-lakes"
title: "Knowledge Ops — Role-Scoped Lakes Spec"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.392Z"
source: "specs/epics/knowledge-ops-role-scoped-lakes.md"
points: null
category: "epics"
---

# Knowledge Ops — Role-Scoped Lakes Spec

> Source: `specs/epics/knowledge-ops-role-scoped-lakes.md`

## Current Canonical Reading

- state: current conceptual spec, terminology refreshed toward Knoxx
- canonical ingestion path today: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`
- legacy preset labels in older notes should map to current built-in Knoxx roles as:
  - `knowledge` -> `knowledge_worker`
  - `development` -> `developer`
  - `analyst` -> `data_analyst`
  - `owner` / `cto` -> `org_admin` or `system_admin` depending scope

> *Not one corpus. A federation of lakes. Each role sees the slices it actually needs.*

---
## Purpose

Triage 2026-05-29: Well-specified epic with clear purpose, concrete scope, and live code anchors already in place (worker.clj classifies files into lakes; OpenPlanner search already supports source/kind/project filtering). Next step is UI lake-aware search presets — actionable and unblocked. Relevant to the core knoxx/openplanner knowledge-graph product direction. No hard external dependencies. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
