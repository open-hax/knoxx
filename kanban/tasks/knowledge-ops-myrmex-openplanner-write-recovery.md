---
uuid: "knoxx-knowledge-ops-myrmex-openplanner-write-recovery"
title: "Knowledge Ops — Myrmex OpenPlanner Write Recovery"
status: review
priority: "P1"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-myrmex-openplanner-write-recovery.md"
points: 3
category: "tasks"
---
# Knowledge Ops — Myrmex OpenPlanner Write Recovery

> Source: `specs/tasks/knowledge-ops-myrmex-openplanner-write-recovery.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 3

Date: 2026-04-05
Status: ready
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 3

## Purpose

Restore Myrmex's ability to write crawl graph events into OpenPlanner and leave backpressure pause.

## Problem

The live `myrmex` runtime is repeatedly reporting:

- OpenPlanner health transport failures
- write transport failures
- sustained pause under backpressure
- a large frontier with pending writes not draining

## Goals

1. Verify Myrmex can reach OpenPlanner from the current local stack.
2. Fix any base URL, auth, or network-path issues blocking writes.
3. Confirm writes succeed and backpressure recovers.

## Non-Goals

1. Frontier-scoring redesign.
2. ACO behavior changes.
3. Graph-Weaver presentation changes.

## Affected files / surfaces

- `services/knoxx/docker-compose.yml`
- Myrmex repo/runtime config referenced by the stack
- OpenPlanner health/write contract if the issue is contract drift

## Verification

1. Myrmex logs show successful health checks and writes.
2. Backpressure streak no longer grows indefinitely.
3. Pending writes drain.
4. Frontier resumes moving.

## Definition of done

- Myrmex can reliably write into OpenPlanner in local dev.
- OpenPlanner backpressure becomes exceptional, not steady-state.

---
**Breakdown 2026-05-29 (accepted → ready):** 3sp, P1. Scope confirmed — runtime connectivity fix: (1) inspect Myrmex transport config and OpenPlanner URL/auth in `docker-compose.yml`, (2) identify the failing hop (health check vs write, network path vs auth vs contract drift), (3) fix and verify logs clear. No algorithm changes. Exit signal: Myrmex logs show successful writes and backpressure streak stops growing. Ready for implementation.

Many of these older ones from march/april are likely done, but never progressed forward. I'm moving these to review
---
