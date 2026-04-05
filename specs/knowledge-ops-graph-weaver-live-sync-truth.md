# Knowledge Ops — Graph-Weaver Live Sync Truth

Date: 2026-04-05
Status: next
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 5

## Purpose

Ensure Graph-Weaver in `openplanner-graph` mode reflects current canonical OpenPlanner graph state rather than stale persisted state.

## Problem

The live local stack currently shows a contradiction:

- OpenPlanner graph export is empty
- Graph-Weaver reports tens of thousands of nodes/edges
- Graph-Weaver status also reports failed OpenPlanner sync

This makes the graph workbench visually useful but architecturally untrustworthy.

## Goals

1. Identify exactly which state Graph-Weaver is rendering when OpenPlanner sync fails.
2. Prevent stale graph state from being mistaken for canonical current truth.
3. Make degraded/stale fallback explicit if fallback behavior is retained.

## Non-Goals

1. Removing all persisted Graph-Weaver state.
2. Replacing Graph-Weaver with a new tool.
3. Changing canonical OpenPlanner graph schema.

## Affected files / surfaces

- `orgs/octave-commons/graph-weaver/src/server.ts`
- `orgs/octave-commons/graph-weaver/src/openplanner-graph.ts`
- `orgs/octave-commons/graph-weaver/src/persist.ts`
- `orgs/octave-commons/graph-weaver/public/*` or status/UI surfaces if degraded mode must be shown

## Verification

1. In `openplanner-graph` mode, Graph-Weaver can be shown to match current OpenPlanner export.
2. If sync fails, status/UI clearly states whether displayed graph is stale fallback.
3. Workbench users can distinguish canonical current state from cached convenience state.

## Definition of done

- Graph-Weaver truth source is explicit and testable.
- `openplanner-graph` mode is trustworthy by default or loudly degraded.
