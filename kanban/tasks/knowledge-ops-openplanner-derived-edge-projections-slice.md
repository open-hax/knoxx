---
uuid: "knoxx-knowledge-ops-openplanner-derived-edge-projections-slice"
title: "Knowledge Ops — OpenPlanner Derived Edge Projections Slice (Phase 1)"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
points: 5
category: tasks
---

# Knowledge Ops — OpenPlanner Derived Edge Projections Slice (Phase 1)

> Parent: `knoxx-knowledge-ops-adaptive-web-frontier-and-multiscale-backbone`
> Points: 5
> Blocked on: `knoxx-knowledge-ops-myrmex-openplanner-write-recovery`, `knoxx-knowledge-ops-graph-memory-runtime-smoke-e2e`, `knoxx-knowledge-ops-knoxx-graph-query-contract-v1`

## Purpose

Add the first derived, non-destructive web-edge projection slice on top of raw canonical graph receipts in OpenPlanner. Establishes the projection table foundation (web_edge_receipts_v1, web_page_productivity_v1, web_host_block_memory_v1) that Phases 2-3 depend on.

## Problem

Raw graph receipts are necessary for truth and provenance, but higher-level retrieval and frontier decisions need view-level derived state such as salience, bridge edges, or discovery-friendly slices. Currently no derived projection infrastructure exists in OpenPlanner.

## Goals

1. Keep raw `graph.node` / `graph.edge` receipts authoritative.
2. Add one derived projection/view family that is recomputable.
3. Expose that view through graph export/query semantics without mutating raw truth.

## Non-Goals

1. Full multiscale backbone system (Phase 3).
2. Full daimoi/ACO weighting.
3. Replacing raw graph export.

## Affected files

- `orgs/open-hax/openplanner/specs/openplanner-web-edge-salience-and-backbone-projections.md`
- `orgs/open-hax/openplanner/src/routes/v1/graph.ts`
- New projection/materialization helpers in OpenPlanner (e.g., `src/lib/web-projections.ts`)

## Definition of done

- OpenPlanner supports at least one useful non-destructive derived edge-view slice queryable via `GET /v1/graph/export?edgeView=<view>`.
- Raw graph export remains available and unchanged.
- One declared derived edge view is recomputable from canonical receipts.
- `pnpm test` passes in `orgs/open-hax/openplanner`.

## Gate

Promote to ready only after `knoxx-knowledge-ops-myrmex-openplanner-write-recovery`, `knoxx-knowledge-ops-graph-memory-runtime-smoke-e2e`, and `knoxx-knowledge-ops-knoxx-graph-query-contract-v1` reach DONE and the projection type (salience / bridge / discovery) is decided by owner.
