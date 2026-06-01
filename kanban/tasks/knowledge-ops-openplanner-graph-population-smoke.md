---
uuid: "knoxx-knowledge-ops-openplanner-graph-population-smoke"
title: "Knowledge Ops — OpenPlanner Graph Population Smoke"
status: ready
priority: P1
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-openplanner-graph-population-smoke.md"
points: 5
category: tasks
---

# Knowledge Ops — OpenPlanner Graph Population Smoke

> Source: `specs/tasks/knowledge-ops-openplanner-graph-population-smoke.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 5

Date: 2026-04-05
Status: ready
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 5

## Purpose

Prove that the canonical OpenPlanner runtime can hold, export, and query non-empty graph state in the current Mongo-backed local deploy.

## Problem

The live OpenPlanner runtime currently returns:

- `nodeCount: 0`
- `edgeCount: 0`
- empty `graph/export`
- empty `graph/query`

Even though upstream producers and graph workbench expectations assume canonical graph data exists.

## Goals

1. Seed or ingest a minimal known graph fixture into the live OpenPlanner runtime.
2. Verify `graph/stats`, `graph/export`, and `graph/query` all return expected data.
3. Ensure this works in the active MongoDB runtime path.

## Non-Goals

1. Solving Graph-Weaver sync in this spec.
2. Solving adaptive traversal.
3. Solving all producer pipelines at once.

## Affected files / surfaces

- `orgs/open-hax/openplanner/src/routes/v1/graph.ts`
- `orgs/open-hax/openplanner/src/tests/openplanner-api.test.ts`
- `services/openplanner/README.md` if smoke commands need documentation

## Verification

1. `GET /v1/graph/stats` returns non-zero node/edge counts.
2. `GET /v1/graph/export?...` returns known seeded nodes and edges.
3. `GET /v1/graph/query?...` returns expected graph hits for seeded content.
4. The smoke path is runnable in the local dev stack, not just unit tests.

## Definition of done

- OpenPlanner graph runtime is proven non-empty under current storage mode.
- A repeatable smoke path exists for future regressions.

## Breakdown

Seed a minimal known graph fixture directly into the Mongo-backed OpenPlanner runtime (bypassing write-recovery dependency), then verify the three live routes: GET /v1/graph/stats returns non-zero counts, GET /v1/graph/export returns seeded nodes/edges, and GET /v1/graph/query returns expected hits. Wire a repeatable smoke invocation into openplanner-api.test.ts and document the seed commands in services/openplanner/README.md. The soft dependency on knowledge-ops-myrmex-openplanner-write-recovery is circumvented by the fixture-seeding path, so no hard blocker exists.

---

**Triage 2026-05-29 (incoming → accepted):** P1. Well-defined with clear verification steps against three live graph routes. 5sp score confirmed. Soft dependency on `knowledge-ops-myrmex-openplanner-write-recovery` (OpenPlanner must be reachable), but can proceed independently with a seed fixture. Accepted.

---

**Triage 2026-05-29 (accepted → ready):** All four ready-gate criteria satisfied: 5sp, unambiguous DoD (three named route assertions + repeatable smoke path), clearly bounded scope with explicit non-goals, and no hard inter-task dependency. The soft dependency on knowledge-ops-myrmex-openplanner-write-recovery is not blocking — the task explicitly allows seeding a fixture directly, and the write-recovery task is READY/in-flight rather than a prerequisite. Promoted to ready.
