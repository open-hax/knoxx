# Knowledge Ops — Knoxx Graph Query Contract v1

Date: 2026-04-05
Status: next
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
