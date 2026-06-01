---
uuid: "knoxx-knowledge-ops-docs-source-of-truth-normalization"
title: "Knowledge Ops — Docs Source-of-Truth Normalization"
status: ready
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-docs-source-of-truth-normalization.md"
points: 2
category: tasks
---

# Knowledge Ops — Docs Source-of-Truth Normalization

> Source: `specs/tasks/knowledge-ops-docs-source-of-truth-normalization.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 2

Date: 2026-04-05
Status: ready
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 2

## Purpose

Normalize the docs so current readers stop getting conflicting stories about what Knoxx, OpenPlanner, and Graph-Weaver are today.

## Problem

The current doc set mixes:

- stale READMEs
- donor-era knowledge-ops docs
- current source/runtime behavior

This increases planning overhead and causes architectural drift in future work.

## Goals

1. Point readers at the reconciliation spec as the current-state anchor.
2. Correct obviously stale backend/runtime descriptions.
3. Make source-home vs runtime-home explicit where needed.

## Non-Goals

1. Rewriting the entire knowledge-ops corpus.
2. Perfecting product messaging.
3. Deleting historical donor material.

## Affected files

- `orgs/open-hax/knoxx/README.md`
- `orgs/open-hax/knoxx/specs/README.md`
- `orgs/open-hax/openplanner/README.md`
- `services/knoxx/README.md` as needed

## Verification

1. README-level readers land on the right current-state docs quickly.
2. Knoxx backend is no longer described as the old Python/FastAPI implementation where that is false.

## Definition of done

- The obvious current-state doc contradictions are removed or superseded.

## Breakdown

Scope is tightly bounded to README-level edits across 3-4 named files: `orgs/open-hax/knoxx/README.md`, `orgs/open-hax/knoxx/specs/README.md`, `orgs/open-hax/openplanner/README.md`, and `services/knoxx/README.md` as needed. Implementation steps: (1) add a pointer to the reconciliation spec as the current-state anchor in each file, (2) strike or correct any Python/FastAPI backend references that no longer reflect runtime, (3) annotate source-home vs runtime-home where ambiguous. No code changes, no inter-task dependencies — can be picked up immediately against main.

---

**Triage 2026-05-29 (incoming → accepted):** P2. 2sp score confirmed — bounded to README-level edits across 3-4 files, no implementation work. Can be picked up independently of other graph recovery tasks. Accepted.

---

**Triage 2026-05-29 (accepted → ready):** All ready-gate criteria pass. 2sp confirmed, DoD is unambiguous (remove obvious current-state contradictions), affected files are explicitly named, no hard inter-task dependency blocks the work. Promoting to ready.
