---
uuid: "knoxx-knowledge-ops-openplanner-salience-backbone-materialization"
title: "Knowledge Ops — OpenPlanner Salience + Backbone Materialization (Phases 2-3)"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 5
category: tasks
---

# Knowledge Ops — OpenPlanner Salience + Backbone Materialization (Phases 2-3)

> Parent: `knoxx-knowledge-ops-adaptive-web-frontier-and-multiscale-backbone`
> Points: 5
> Sequenced after: `knoxx-knowledge-ops-openplanner-derived-edge-projections-slice` (Phase 1)

## Purpose

Implement Phase 2 (edge salience materialization) and Phase 3 (backbone membership materialization) in OpenPlanner once Phase 1 projection tables are in place.

## Problem

Phase 1 adds raw projection tables (`web_edge_receipts_v1`, `web_page_productivity_v1`, `web_host_block_memory_v1`). Without Phase 2-3, there is no derived scoring state — the adaptive frontier and graph-weaver-aco cannot make routing decisions based on edge quality or structural importance.

## Goals

1. Compute and persist `web_edge_salience_v1` from Phase 1 projection inputs.
2. Compute and persist `web_backbone_membership_v1` supporting backbone names: `discovery`, `structural`, `evidence`, `bridge`.
3. Implement local-significance edge filtering (disparity filter or noise-corrected backbone).
4. Make bridge rescue explicit and queryable (not a hidden heuristic).
5. Wire score versioning and timestamps so staleness can be detected.

## Non-Goals

- Explainability HTTP endpoints (Phase 4, separate task).
- Modifying raw graph receipts.
- Myrmex-side adaptive expand policy (separate repo).

## Affected files

- `orgs/open-hax/openplanner/src/lib/indexing.ts` or a new `src/lib/web-projections.ts` — materialization workers
- `orgs/open-hax/openplanner/src/routes/v1/graph.ts` — extend `edgeView` query param to support `discovery`, `structural`, `evidence`, `bridge`
- `orgs/open-hax/openplanner/src/lib/types.ts` — add types for salience and backbone entities
- Schema migration file for `web_edge_salience_v1` and `web_backbone_membership_v1` tables
- `orgs/open-hax/openplanner/specs/openplanner-web-edge-salience-and-backbone-projections.md` — implementation spec reference

## Verification

1. `GET /v1/graph/export?projects=web&edgeView=discovery` returns a different (smaller) edge set than `edgeView=raw`.
2. `GET /v1/graph/export?projects=web&edgeView=bridge` returns only bridge-rescued edges.
3. `web_edge_salience_v1` rows have `score_version` and `updated_at` populated.
4. `web_backbone_membership_v1` rows have `bridge_rescue` column queryable.
5. Raw export (`edgeView=raw`) remains available and unchanged.

## Definition of done

- `web_edge_salience_v1` and `web_backbone_membership_v1` tables exist and are populated by a recomputable worker.
- `GET /v1/graph/export?edgeView=discovery` and `edgeView=bridge` return meaningful filtered results.
- Bridge edges are flagged with `bridge_rescue=true` (not silently folded into other views).
- All existing graph export tests continue to pass: `pnpm test` in `orgs/open-hax/openplanner`.
