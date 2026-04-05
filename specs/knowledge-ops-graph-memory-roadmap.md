# Knowledge Ops — Graph Memory Roadmap

Date: 2026-04-05
Status: active roadmap
Parent: `knowledge-ops-graph-memory-reconciliation.md`

## Purpose

Turn the graph-memory reconciliation epic into an execution roadmap grounded in the current local Knoxx + OpenPlanner + Graph-Weaver runtime.

This roadmap answers:

- what to do first
- what must be true before the next slice starts
- which child specs can move now
- which later slices stay explicitly deferred

## Roadmap rules

1. Use Fibonacci points only.
2. No executable child spec may exceed 5 points.
3. Current source code and live runtime evidence beat stale docs.
4. Runtime coherence comes before traversal cleverness.
5. OpenPlanner is canonical graph truth; Graph-Weaver is the workbench; Knoxx exposes bounded agent-facing graph and memory surfaces.

## Current starting state

The roadmap starts from the currently verified local runtime:

- Knoxx backend health is degraded and `/health/knoxx` returns `503`
- `kms-ingestion` is failing on `ingest-via-openplanner!` arity mismatch
- Myrmex is paused behind OpenPlanner write/backpressure failure
- OpenPlanner graph export is empty
- Graph-Weaver shows stale non-empty graph while `openplanner-graph` sync is failing

## Milestones

| Order | Milestone | Points | Child specs | Exit signal |
|------|-----------|--------|-------------|-------------|
| M0 | Runtime unblockers | 8 | `knowledge-ops-kms-openplanner-ingest-arity-fix.md` (2), `knowledge-ops-knoxx-health-route-coherence.md` (3), `knowledge-ops-myrmex-openplanner-write-recovery.md` (3) | Knoxx health semantics are stable, KMS ingest bug is gone, and Myrmex can write to OpenPlanner again. |
| M1 | Canonical graph proof | 5 | `knowledge-ops-openplanner-graph-population-smoke.md` (5) | OpenPlanner `graph/stats`, `graph/export`, and `graph/query` all return known non-empty graph data in the live Mongo path. |
| M2 | Workbench truth | 5 | `knowledge-ops-graph-weaver-live-sync-truth.md` (5) | Graph-Weaver either matches current OpenPlanner graph truth or loudly declares degraded stale fallback. |
| M3 | End-to-end guardrail | 3 | `knowledge-ops-graph-memory-runtime-smoke-e2e.md` (3) | One smoke path proves producer → OpenPlanner → Graph-Weaver → Knoxx coherence and identifies the broken hop on failure. |
| M4 | Contract freeze and docs | 5 | `knowledge-ops-knoxx-graph-query-contract-v1.md` (3), `knowledge-ops-docs-source-of-truth-normalization.md` (2) | Agent-facing graph semantics are frozen around current reality and top-level docs stop pointing readers at stale architecture stories. |
| M5 | Derived graph views | 5 | `knowledge-ops-openplanner-derived-edge-projections-slice.md` (5) | At least one derived edge-view family is queryable without mutating canonical raw graph receipts. |
| M6 | Adaptive seam | 2 | `knowledge-ops-adaptive-expand-policy-seam.md` (2) | A policy seam exists behind bounded graph expansion with no public contract churn. |
| M7 | Adaptive telemetry | 2 | `knowledge-ops-adaptive-expand-policy-telemetry.md` (2) | Expansion operations emit enough structured telemetry to compare future policies against the baseline. |

## Dependency map

### Must happen first

- `knowledge-ops-kms-openplanner-ingest-arity-fix.md`
- `knowledge-ops-knoxx-health-route-coherence.md`
- `knowledge-ops-myrmex-openplanner-write-recovery.md`

These unblock the producer and service path before higher-level graph claims can be trusted.

### Depends on canonical graph reality

- `knowledge-ops-openplanner-graph-population-smoke.md`
- `knowledge-ops-graph-weaver-live-sync-truth.md`
- `knowledge-ops-graph-memory-runtime-smoke-e2e.md`

These should not be treated as complete until live OpenPlanner graph data is demonstrably real.

### Depends on runtime coherence

- `knowledge-ops-knoxx-graph-query-contract-v1.md`
- `knowledge-ops-docs-source-of-truth-normalization.md`

Do not freeze contracts or docs around broken runtime behavior.

### Explicitly later

- `knowledge-ops-openplanner-derived-edge-projections-slice.md`
- `knowledge-ops-adaptive-expand-policy-seam.md`
- `knowledge-ops-adaptive-expand-policy-telemetry.md`

These only make sense once the canonical graph path is trustworthy and the v1 contract is stable.

## Immediate board order

Pull work in this order unless a fresh runtime check reveals a tighter blocker:

1. `knowledge-ops-kms-openplanner-ingest-arity-fix.md` — 2
2. `knowledge-ops-knoxx-health-route-coherence.md` — 3
3. `knowledge-ops-myrmex-openplanner-write-recovery.md` — 3
4. `knowledge-ops-openplanner-graph-population-smoke.md` — 5
5. `knowledge-ops-graph-weaver-live-sync-truth.md` — 5
6. `knowledge-ops-graph-memory-runtime-smoke-e2e.md` — 3
7. `knowledge-ops-knoxx-graph-query-contract-v1.md` — 3
8. `knowledge-ops-docs-source-of-truth-normalization.md` — 2
9. `knowledge-ops-openplanner-derived-edge-projections-slice.md` — 5
10. `knowledge-ops-adaptive-expand-policy-seam.md` — 2
11. `knowledge-ops-adaptive-expand-policy-telemetry.md` — 2

## Phase gates

### Gate A — runtime coherence restored

Before leaving M0 + M1, all of these should be true:

- Knoxx health is no longer failing for opaque dependency-fetch reasons
- KMS ingestion is writing again
- Myrmex is no longer permanently paused behind write backpressure
- OpenPlanner graph endpoints show real data

### Gate B — graph truth is trustworthy end to end

Before leaving M2 + M3, all of these should be true:

- Graph-Weaver is trustworthy in `openplanner-graph` mode
- there is one repeatable end-to-end smoke path
- a failing hop can be identified quickly

### Gate C — contract can stabilize

Before starting later graph intelligence work, all of these should be true:

- `graph_query` v1 is documented against actual runtime behavior
- top-level docs send readers to the current canonical story
- graph-memory work no longer depends on stale README lore

## What is deliberately not in the current roadmap slice

The roadmap explicitly defers:

- daimoi / semantic-gravity / ACO traversal policy work
- large graph-ranking experiments
- major UI redesigns unrelated to graph truth
- speculative GraphRAG abstraction layers that do not fix current runtime coherence

## Definition of done for the roadmap’s active tranche

Treat the active graph-memory tranche as complete when M0 through M4 are green:

- producers can write
- OpenPlanner graph truth is populated and queryable
- Graph-Weaver reflects canonical truth or clearly signals degraded fallback
- Knoxx has a bounded documented graph contract
- docs stop fighting runtime reality

At that point, derived views and adaptive expansion become sane follow-on work instead of architecture cosplay.
