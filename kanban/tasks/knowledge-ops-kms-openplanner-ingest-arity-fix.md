---
uuid: "knoxx-knowledge-ops-kms-openplanner-ingest-arity-fix"
title: "Knowledge Ops — KMS OpenPlanner Ingest Arity Fix"
status: review
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-kms-openplanner-ingest-arity-fix.md"
points: 2
category: tasks
---
# Knowledge Ops — KMS OpenPlanner Ingest Arity Fix

> Source: `specs/tasks/knowledge-ops-kms-openplanner-ingest-arity-fix.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 2

Date: 2026-04-05
Status: review
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 2

## Purpose

Restore the Knoxx ingestion worker's ability to write canonical document and graph events into OpenPlanner.

## Problem

The live `kms-ingestion` runtime is repeatedly logging:

- `Wrong number of args (7) passed to: kms-ingestion.jobs.worker/ingest-via-openplanner!`

---

**Triage 2026-05-28 (accepted → review):** The arity is now consistent in source. `ingest-via-openplanner!` is defined with 7 params in `ingestion/src/kms_ingestion/jobs/ingest_support.clj` (≈line 97) and the sole call site in `jobs/worker.clj` (≈line 85) passes exactly 7 args. The defect described in this card is resolved at the code level. Moving to review rather than done because this is a runtime-recovery task — the owner should confirm the live `kms-ingestion` worker no longer logs the arity error before closing.

---

