---
uuid: "knoxx-pii-classification-schema-ingestion-hook"
title: "PII Classification Schema and Ingestion Detection Hook"
status: icebox
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# PII Classification Schema and Ingestion Detection Hook

Parent: `knoxx-knowledge-ops-pii-handling-protocol`

## Purpose

Define the internal PII classification label set (e.g. `pii:none`, `pii:quasi`, `pii:direct`, `pii:sensitive`) and wire a detection/classification step into the ingestion pipeline so every ingested document receives a `pii_level` label before being persisted.

## Affected Files

- `ingestion/src/kms_ingestion/jobs/worker.clj` — add classification call after content read, before graph push
- `ingestion/src/kms_ingestion/db.clj` — add `pii_level` column to `ingestion_file_state`
- New ns: `ingestion/src/kms_ingestion/pii/classify.clj` — regex/heuristic classifier

## Definition of Done

1. `ingestion_file_state` table has a `pii_level TEXT NOT NULL DEFAULT 'pii:none'` column (migration applied).
2. `kms-ingestion.pii.classify/classify-content` function exists and returns one of `#{"pii:none" "pii:quasi" "pii:direct" "pii:sensitive"}`.
3. `worker.clj` calls classifier and stores result via `db/update-file-pii-level!`.
4. Unit test in `ingestion/test/kms_ingestion/pii/classify_test.clj` covering at least 4 fixture strings.
5. `clj -M:test` passes with zero failures.

---
Triage 2026-05-29: Bounded 3sp slice with a fully-specified DoD — DB migration column, new `pii/classify.clj` namespace, worker wiring, and a 4-fixture unit test suite — all named files exist in the ingestion subproject and no external dependencies are outstanding. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
