---
uuid: "knoxx-knowledge-ops-graph-memory-reconciliation"
title: "Knowledge Ops — Graph Memory Reconciliation Spec"
status: "icebox"
priority: "P2"
labels: ["epics"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/epics/knowledge-ops-graph-memory-reconciliation.md"
points: null
category: "epics"
---

# Knowledge Ops — Graph Memory Reconciliation Spec

> Source: `specs/epics/knowledge-ops-graph-memory-reconciliation.md`

Date: 2026-04-05
Status: epic wrapper / current-state reconciliation

## Purpose

Reconcile the intended GraphRAG / graph-memory architecture with the system that actually exists today across:

- `orgs/open-hax/knoxx/`
- `orgs/open-hax/openplanner/`
- `orgs/octave-commons/graph-weaver/`
- `services/knoxx/`
- `services/openplanner/`

This spec is the canonical current-state bridge between:

---
Triage 2026-05-29: Well-formed epic wrapper reconciling intended GraphRAG/graph-memory architecture with actual live system state across knoxx, openplanner, and graph-weaver. Purpose is clear, scope is bounded (four phases, child specs capped at 5sp), and the underlying runtime problems are real and documented with live observations (empty OpenPlanner graph, knoxx-backend unhealthy, KMS arity bug, Myrmex backpressure). Child tasks are already decomposed and actively in motion (knoxx-health-route-coherence done, kms-openplanner-ingest-arity-fix in review, openplanner-graph-population-smoke ready). No external hard blockers — all dependencies are internal and in progress. Verdict: accepted (P2).
---
