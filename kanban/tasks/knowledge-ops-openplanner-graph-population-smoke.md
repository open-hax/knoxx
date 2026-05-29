---
uuid: "knoxx-knowledge-ops-openplanner-graph-population-smoke"
title: "Knowledge Ops — OpenPlanner Graph Population Smoke"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-openplanner-graph-population-smoke.md"
points: 5
category: tasks
---

# Knowledge Ops — OpenPlanner Graph Population Smoke

> Source: `specs/tasks/knowledge-ops-openplanner-graph-population-smoke.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 5

Date: 2026-04-05
Status: next
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 5

## Purpose

Prove that the canonical OpenPlanner runtime can hold, export, and query non-empty graph state in the current Mongo-backed local deploy.

## Problem

The live OpenPlanner runtime currently returns:

- `nodeCount: 0`
- `edgeCount: 0`
