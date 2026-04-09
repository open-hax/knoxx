# Knowledge Ops — OpenPlanner Graph Population Smoke

Date: 2026-04-05
Status: next
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
