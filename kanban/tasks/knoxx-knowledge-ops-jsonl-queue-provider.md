---
uuid: "knoxx-knowledge-ops-jsonl-queue-provider"
title: "Implement JSONL file-backed job queue provider for knowledge ops"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Implement JSONL file-backed job queue provider for knowledge ops

> Parent epic: `knoxx-knowledge-ops-deploy-self-hosted`
> Points: 2

## Purpose

Replace any cloud or in-memory queue dependency with a durable JSONL file-backed job queue so the ingestion pipeline can enqueue, poll, and acknowledge jobs without Redis or a managed broker, activated via `QUEUE_PROVIDER=jsonl`.

## Scope

- Implement a JSONL queue adapter: pending jobs appended to `<QUEUE_DIR>/pending.jsonl`, in-flight tracked in `<QUEUE_DIR>/inflight.jsonl`, completed/failed archived to `<QUEUE_DIR>/done.jsonl`
- Expose `QUEUE_DIR` env var (default `./data/queue`)
- Adapter must satisfy the queue protocol: `enqueue(job)`, `dequeue() → job | nil`, `ack(job-id)`, `nack(job-id, reason)`
- File locking (or atomic rename) must prevent double-dequeue under concurrent workers
- Write unit tests covering enqueue/dequeue/ack and nack-retry flow

## Definition of done

- `QUEUE_PROVIDER=jsonl` enqueues and dequeues jobs durably across process restarts
- Nacked jobs are re-queued and not lost
- Unit tests pass (`pnpm test` in `backend/`)

## Notes

Split from parent epic `knoxx-knowledge-ops-deploy-self-hosted` on 2026-05-30.
