---
uuid: "knoxx-pii-deletion-retention-protocol"
title: "PII Deletion and Retention Protocol"
status: ready
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# PII Deletion and Retention Protocol

Parent: `knoxx-knowledge-ops-pii-handling-protocol`

## Purpose

Implement a hard-delete API endpoint and a scheduled retention sweep that purges documents classified as `pii:direct` or `pii:sensitive` after a configurable retention window (default 90 days), and confirm deletion propagates to the graph store.

## Affected Files

- `ingestion/src/kms_ingestion/db.clj` — add `get-expired-pii-files` and `hard-delete-file!` helpers
- `ingestion/src/kms_ingestion/api/routes.clj` — add `DELETE /api/files/:file_id` with tenant + pii_level check
- `ingestion/src/kms_ingestion/jobs/worker.clj` or new ns `jobs/retention_sweep.clj` — scheduled sweep using `pii_level` + `last_ingested_at`
- `ingestion/src/kms_ingestion/graph.clj` — ensure graph node deletion is called on hard delete

## Definition of Done

1. `DELETE /api/files/:file_id` returns 204 and removes the row from `ingestion_file_state`; cross-tenant deletion attempt returns 403.
2. Retention sweep function deletes all rows where `pii_level IN ('pii:direct','pii:sensitive') AND last_ingested_at < NOW() - INTERVAL '<retention_days> days'`.
3. Graph deletion event is emitted for each purged file (verify via event-bus spy in test).
4. `clj -M:test` passes.

---
Triage 2026-05-29: Freshly-split epic subtask with a concrete DoD covering a hard-delete API endpoint, a scheduled retention sweep, and graph propagation — all scoped to specific Clojure namespaces in the ingestion layer with no external dependencies. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
