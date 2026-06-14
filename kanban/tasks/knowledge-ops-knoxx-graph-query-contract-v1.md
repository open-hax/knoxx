---
uuid: "knoxx-knowledge-ops-knoxx-graph-query-contract-v1"
title: "Knowledge Ops — Knoxx Graph Query Contract v1"
status: review
priority: P1
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-knoxx-graph-query-contract-v1.md"
points: 3
category: tasks
---
# Knowledge Ops — Knoxx Graph Query Contract v1

> Source: `specs/tasks/knowledge-ops-knoxx-graph-query-contract-v1.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 3

Date: 2026-04-05
Status: ready
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 3

## Purpose

Freeze a small, stable, agent-facing graph query contract around the graph and memory surfaces that already exist.

## Problem

The current system has working primitives in multiple places, but the conceptual contract is still fuzzy. Without freezing that contract, future traversal or memory work will churn APIs and prompt guidance.

## Goals

1. Define the first stable Knoxx graph-facing contract around existing behavior.
2. Keep the contract bounded and algorithm-agnostic.
3. Map it cleanly onto current OpenPlanner graph routes and Knoxx tool surfaces.

## Non-Goals

1. Adding adaptive traversal yet.
2. Exposing Graph-Weaver internals directly to agents.
3. Redesigning semantic query or memory query surfaces.

## Contract focus

`graph_query` v1 should remain about:

- search
- bounded incident edge retrieval
- lake scoping
- node-type scoping
- textual result summarization

Traversal policy remains an implementation detail behind later versions.

## Affected files / surfaces

- `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`
- `orgs/open-hax/openplanner/src/routes/v1/graph.ts`
- adjacent docs/specs that describe graph tool usage

## Verification

1. The v1 contract is documented in one place.
2. Knoxx prompt/tool metadata matches the documented contract.
3. OpenPlanner route semantics line up with the documented tool behavior.

## Definition of done

- Agents have one bounded graph contract to target.
- Future adaptive expansion can land behind the same interface.

---

**Breakdown 2026-05-29 (accepted → ready):** 3sp, P1. Scope confirmed — a documentation-first task: (1) read existing `core.cljs` graph tool and OpenPlanner `/v1/graph/*` routes, (2) write a single contract doc that reconciles them, (3) update Knoxx tool metadata to reference the contract. No new code required unless tool metadata is out of sync. Exit signal: one canonical contract document exists and tool metadata references it. Ready for implementation.
