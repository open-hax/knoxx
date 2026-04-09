# Knowledge Ops — OpenPlanner Derived Edge Projections Slice

Date: 2026-04-05
Status: later
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 5

## Purpose

Add the first derived, non-destructive web-edge projection slice on top of raw canonical graph receipts in OpenPlanner.

## Problem

Raw graph receipts are necessary for truth and provenance, but higher-level retrieval and frontier decisions need view-level derived state such as salience, bridge edges, or discovery-friendly slices.

## Goals

1. Keep raw `graph.node` / `graph.edge` receipts authoritative.
2. Add one derived projection/view family that is recomputable.
3. Expose that view through graph export/query semantics without mutating raw truth.

## Non-Goals

1. Full multiscale backbone system.
2. Full daimoi/ACO weighting.
3. Replacing raw graph export.

## Affected files / surfaces

- `orgs/open-hax/openplanner/specs/openplanner-web-edge-salience-and-backbone-projections.md`
- `orgs/open-hax/openplanner/src/routes/v1/graph.ts`
- any new projection/materialization helpers required in OpenPlanner

## Verification

1. Raw graph export remains available.
2. One declared derived edge view becomes queryable.
3. Derived state is recomputable from canonical receipts.

## Definition of done

- OpenPlanner supports at least one useful non-destructive derived edge-view slice.
