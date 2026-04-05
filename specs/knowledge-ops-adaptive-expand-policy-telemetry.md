# Knowledge Ops — Adaptive Expand Policy Telemetry

Date: 2026-04-05
Status: later
Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
Story points: 2

## Purpose

Add structured telemetry for bounded graph expansion so future adaptive policies can be compared against the baseline using evidence rather than intuition.

## Problem

Even with a policy seam, future adaptive traversal will be guesswork unless the system records which policy ran, what bounds were applied, and what result shape came back.

## Goals

1. Emit structured telemetry for expansion requests and outcomes.
2. Record enough context to compare default and future policies.
3. Keep telemetry out of the public agent-facing contract.

## Non-Goals

1. Real-time policy optimization.
2. Building a full observability dashboard.
3. Exposing internal scoring details directly to agents.

## Telemetry minimums

At minimum, record:

- operation type
- active policy name
- applied bounds / limits
- result counts or summary shape
- duration / failure class

## Affected files / surfaces

- `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`
- any structured log / metric / receipt surface used by Knoxx graph operations
- adjacent docs/specs describing graph query behavior

## Verification

1. Expansion operations emit structured telemetry under the default policy.
2. Telemetry can distinguish policy choice, bounds, and outcome shape.
3. Agent-facing tool semantics remain unchanged.

## Definition of done

- Future adaptive traversal work has an evidence surface for judging policy quality without redesigning the public graph contract.
