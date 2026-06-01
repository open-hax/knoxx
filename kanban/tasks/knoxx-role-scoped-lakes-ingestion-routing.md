---
uuid: "knoxx-role-scoped-lakes-ingestion-routing"
title: "Role-Scoped Lakes: Verify and extend ingestion lake routing in worker.clj"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Role-Scoped Lakes: Verify and extend ingestion lake routing in worker.clj

> Parent epic: `knoxx-knowledge-ops-role-scoped-lakes`
> Points: 3

## Purpose

Confirm that the ingestion worker in `worker.clj` correctly routes files into all five canonical `devel-*` lakes (`devel-docs`, `devel-code`, `devel-config`, `devel-data`, `devel-events`) and that the OpenPlanner `source_ref.project` field is populated with the right lake name on each ingested item.

## Scope

- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj` — audit the existing file classification logic against the canonical lake rules in the spec (path patterns, extensions, fallback to `docs`)
- Add or correct any missing extension/path rules for `devel-config` (yaml, toml, Dockerfile, .env*) and `devel-data` (.jsonl, .csv, .tsv, .parquet) if absent
- Ensure the resulting OpenPlanner payload sets `source_ref.project` to the lake name (e.g. `devel-code`) and `kind` to the lake type (e.g. `code`)
- Add or update unit tests in the ingestion test suite covering each lake classifier branch

## Definition of done

- Running the ingestion worker against a small fixture directory produces items in OpenPlanner with correct `source_ref.project` values for each lake type
- All five lake names (`devel-docs`, `devel-code`, `devel-config`, `devel-data`, `devel-events`) appear in ingested output for an appropriately varied fixture
- Ingestion unit tests pass (`clj -M:test`) with no regressions
- No files classified into the wrong lake for the common extension cases listed in the spec

## Notes

Split from parent epic `knoxx-knowledge-ops-role-scoped-lakes` on 2026-05-30.
