---
uuid: "knoxx-knowledge-ops-graph-weaver-live-sync-truth"
title: "Knowledge Ops — Graph-Weaver Live Sync Truth"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-graph-weaver-live-sync-truth.md"
points: 5
category: tasks
---

# Knowledge Ops — Graph-Weaver Live Sync Truth

> Source: `specs/tasks/knowledge-ops-graph-weaver-live-sync-truth.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 5

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
