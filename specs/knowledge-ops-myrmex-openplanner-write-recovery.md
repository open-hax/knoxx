# Knowledge Ops — Myrmex OpenPlanner Write Recovery

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
