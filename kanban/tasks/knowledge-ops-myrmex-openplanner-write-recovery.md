---
uuid: "knoxx-knowledge-ops-myrmex-openplanner-write-recovery"
title: "Knowledge Ops — Myrmex OpenPlanner Write Recovery"
status: accepted
priority: P1
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-myrmex-openplanner-write-recovery.md"
points: 3
category: tasks
---
# Knowledge Ops — Myrmex OpenPlanner Write Recovery

> Source: `specs/tasks/knowledge-ops-myrmex-openplanner-write-recovery.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 3

Date: 2026-04-05
Status: next
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 3

## Purpose

Restore Myrmex's ability to write crawl graph events into OpenPlanner and leave backpressure pause.

## Problem

The live `myrmex` runtime is repeatedly reporting:

- OpenPlanner health transport failures
- write transport failures
