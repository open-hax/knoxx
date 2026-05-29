---
uuid: "knoxx-knowledge-ops-adaptive-expand-policy-hook"
title: "Knowledge Ops — Adaptive Expand Policy Hook"
status: incoming
priority: P2
labels: ["epics", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/epics/knowledge-ops-adaptive-expand-policy-hook.md"
points: null
category: epics
---

# Knowledge Ops — Adaptive Expand Policy Hook

> Source: `specs/epics/knowledge-ops-adaptive-expand-policy-hook.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`

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
