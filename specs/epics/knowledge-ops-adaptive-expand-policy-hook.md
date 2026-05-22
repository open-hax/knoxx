# Knowledge Ops — Adaptive Expand Policy Hook

Date: 2026-04-05
Status: later epic wrapper
Parent: `knowledge-ops-graph-memory-reconciliation.md`

## Purpose

Introduce a pluggable expansion-policy hook so future daimoi / semantic-gravity / ACO traversal can land behind a stable bounded graph retrieval contract.

## Epic decomposition

This document is a wrapper for the later adaptive-expansion slice.
Pull the child specs instead of executing this wrapper directly:

- `knowledge-ops-adaptive-expand-policy-seam.md` — 2
- `knowledge-ops-adaptive-expand-policy-telemetry.md` — 2

## Problem

The architecture wants adaptive traversal, but implementing that directly in the current query contract would create churn and couple agents to traversal strategy details.

## Goals

1. Keep graph retrieval bounded.
2. Separate contract from traversal policy.
3. Split the work so the seam and telemetry can be estimated independently using Fibonacci points.

## Non-Goals

1. Shipping full ACO traversal now.
2. Replacing current search/query primitives.
3. Exposing policy internals directly to agents.

## Contract direction

The hook should sit behind bounded operations such as:

- search
- expand
- preview
- write-back

with policy choices hidden behind implementation/runtime config.

## Verification

1. Existing graph query surfaces remain stable.
2. Policy hook can be swapped without changing agent-facing tool semantics.
3. Telemetry exists to support future adaptive traversal decisions.
4. No executable child spec in this slice uses a non-Fibonacci estimate.

## Definition of done

- This wrapper is decomposed into Fibonacci-sized child specs.
- A future adaptive traversal layer can be added without redesigning the graph query contract.
