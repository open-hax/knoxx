---
uuid: "knoxx-pii-tenant-isolation-retrieval-guard"
title: "PII Tenant Isolation — Retrieval Query Guard"
status: ready
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# PII Tenant Isolation — Retrieval Query Guard

Parent: `knoxx-knowledge-ops-pii-handling-protocol`

## Purpose

Enforce that retrieval and query routes never return documents belonging to a different tenant, and that high-PII documents (`pii:direct`, `pii:sensitive`) are only returned to callers with an explicit `read:pii` permission.

## Affected Files

- `ingestion/src/kms_ingestion/api/query_support.clj` — add tenant scoping + pii_level filter
- `ingestion/src/kms_ingestion/db.clj` — add helper `get-files-for-tenant-within-pii-ceiling`
- `backend/src/cljs/knoxx/backend/law/contracts.cljs` — verify/add `read:pii` permission gate

## Definition of Done

1. All DB query helpers in `query_support.clj` accept `tenant-id` and apply it as a WHERE clause — no cross-tenant leakage possible at the SQL level.
2. A call without `read:pii` permission returns HTTP 403 when the result set contains any row with `pii_level` in `#{"pii:direct" "pii:sensitive"}`.
3. Integration test in `ingestion/test/kms_ingestion/api/query_support_test.clj` with two tenants proves isolation.
4. `clj -M:test` passes.

---
Triage 2026-05-29: Concrete 3sp subtask with three named files, a SQL-level tenant-scoping requirement, a 403 gate for read:pii, and an integration test spec — all fully actionable today with no unresolved dependencies. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
