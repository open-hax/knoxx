---
uuid: "knoxx-knowledge-ops-graph-memory-runtime-smoke-e2e"
title: "Knowledge Ops — Graph Memory Runtime Smoke E2E"
status: accepted
priority: P1
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-graph-memory-runtime-smoke-e2e.md"
points: 3
category: tasks
---
# Knowledge Ops — Graph Memory Runtime Smoke E2E

> Source: `specs/tasks/knowledge-ops-graph-memory-runtime-smoke-e2e.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 3

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
