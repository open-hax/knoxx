# Knowledge Ops — Graph Memory Runtime Smoke E2E

Date: 2026-04-05
Status: next
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 3

## Purpose

Add one end-to-end smoke path that proves the local graph-memory stack works across producer, lake, workbench, and consumer surfaces.

## Problem

Today, different layers fail in different ways, and there is no single smoke slice that says:

- producers can write
- OpenPlanner can export/query
- Graph-Weaver can sync
- Knoxx can consume the result

## Goals

1. Define one small cross-service smoke scenario.
2. Run it against the live local stack.
3. Fail fast when graph-memory coherence regresses.

## Non-Goals

1. Full integration-test coverage of every graph feature.
2. UI screenshot testing.
3. Multi-tenant auth coverage beyond smoke assertions.

## Suggested smoke path

1. Emit or ingest a known graph node/edge pair.
2. Verify OpenPlanner stats/export/query.
3. Verify Graph-Weaver status sync and node visibility.
4. Verify Knoxx graph-facing API/tool can retrieve the same slice.

## Verification

- Smoke command/script returns success only when the chain is coherent.
- Failure output identifies the broken hop.

## Definition of done

- A repeatable local runtime smoke exists and is documented.
