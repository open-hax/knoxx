# Knowledge Ops — Adaptive Expand Policy Seam

Date: 2026-04-05
Status: later
Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
Story points: 2

## Purpose

Introduce the smallest internal seam needed to swap graph expansion policy without changing the agent-facing graph query contract.

## Problem

Future adaptive traversal needs a place to plug in, but today that policy seam is not explicit. If adaptive behavior lands directly in query handlers, later experimentation will create contract churn and hidden coupling.

## Goals

1. Define the bounded operations that may consult an expansion policy.
2. Add an internal policy-selection seam or registry behind current behavior.
3. Keep the default policy behaviorally equivalent to the current baseline.

## Non-Goals

1. Shipping daimoi / semantic-gravity / ACO logic.
2. Adding new agent-visible graph parameters.
3. Building telemetry or dashboards in this spec.

## Contract direction

The seam should sit behind bounded operations such as:

- search
- expand
- preview
- write-back preparation

Policy choice remains an internal implementation/runtime concern.

## Affected files / surfaces

- `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`
- `orgs/open-hax/openplanner/src/routes/v1/graph.ts` if bounded expansion is pushed lake-side
- adjacent graph contract docs/specs

## Verification

1. Existing graph query surfaces remain unchanged by default.
2. A distinct expansion-policy seam exists in code.
3. The default policy yields the same bounded behavior as before.

## Definition of done

- Future adaptive policies can be introduced behind one explicit seam instead of modifying agent-facing query semantics.
