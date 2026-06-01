---
uuid: "knoxx-knowledge-ops-kms-query"
title: "Knowledge Ops — KMS Query Service"
status: ready
priority: "P2"
labels: ["epics"]
created_at: "2026-05-28T22:40:14.388Z"
source: "specs/epics/knowledge-ops-kms-query.md"
points: null
category: "epics"
---
# Knowledge Ops — KMS Query Service

> Source: `specs/epics/knowledge-ops-kms-query.md`

## Current Canonical Reading

- state: current retrieval-oriented query slice
- canonical implementation path: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj`
- canonical nginx exposure path: `services/knoxx/config/conf.d/default.conf`
- note: Knoxx's primary interactive chat/runtime surface is now `/api/knoxx/*`; this spec describes the still-useful `/api/query/*` retrieval and synthesis surface

> *One Clojure query surface over many lakes.*

---
## Purpose

Define the `kms-query` surface currently implemented inside the `kms-ingestion` Clojure service. This is the first provider-independent query layer over:
- `devel-*` lakes

Triage 2026-05-29: Epic is grounded in a running Clojure implementation at orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj and exposed via nginx at /api/query/*. Purpose is clear (provider-independent federated FTS query and synthesis surface over devel-* lakes, cephalon-hive, and sintel). Spec was written against live code (specified 2026-04-02). Next step is well-scoped frontend integration (preset selector, multi-lake toggle, results and answer panes). No hard external blockers — OpenPlanner FTS, sintel, and cephalon-hive are confirmed live. Directly relevant to knoxx/openplanner knowledge-graph project. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
