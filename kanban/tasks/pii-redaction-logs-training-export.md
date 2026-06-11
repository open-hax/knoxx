---
uuid: "knoxx-pii-redaction-logs-training-export"
title: "PII Redaction in Logs and Training/SFT Exports"
status: icebox
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# PII Redaction in Logs and Training/SFT Exports

Parent: `knoxx-knowledge-ops-pii-handling-protocol`

## Purpose

Ensure that documents classified as `pii:direct` or `pii:sensitive` are excluded from SFT training exports by default, and that structured log output in the ingestion worker never echoes raw content.

## Affected Files

- `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` — `/api/translations/export/sft` must filter out rows where source document has `pii_level` in `#{"pii:direct" "pii:sensitive"}`
- `ingestion/src/kms_ingestion/jobs/worker.clj` — strip `:content` from any structured log maps before emission
- New helper: `backend/src/cljs/knoxx/backend/infra/pii_filter.cljs`

## Definition of Done

1. `GET /api/translations/export/sft` returns HTTP 200 with no rows where source `pii_level` is `pii:direct` or `pii:sensitive` (verified by integration test fixture).
2. Worker log events never include a `:content` key with more than 64 chars (enforced by truncation helper).
3. Unit test for `pii_filter` in `backend` test suite passes under `pnpm test`.

---
Triage 2026-05-29: Concrete 3sp slice with fully-named affected files, a measurable DoD (integration test fixture for SFT export filtering, 64-char truncation helper for worker logs, unit test for pii_filter), and no external blockers. Verdict: accepted (P2). --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
