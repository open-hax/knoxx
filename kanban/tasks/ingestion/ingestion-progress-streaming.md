---
uuid: "knoxx-ingestion-progress-streaming"
title: "Ingestion Progress Streaming (WebSocket/SSE)"
status: done
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-28T00:00:00Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: 3
category: tasks
---

# Ingestion Progress Streaming

> Parent: `knowledge-ops-ingestion-pipeline`
> Points: 3

## Purpose

Add real-time progress streaming for ingestion jobs via WebSocket and/or SSE so the frontend can show live progress.

## Current State

The worker (`jobs/worker.clj`) tracks `processed_files`, `failed_files`, `chunks_created` on the job record. Progress is only visible by polling `GET /api/ingestion/jobs/:id`.

## What Needs Building

1. **SSE endpoint** — `GET /api/ingestion/jobs/:id/stream` that emits progress events as the worker processes files.
2. **Event format** — `{type, job_id, timestamp, total_files, processed_files, failed_files, chunks_created, percent_complete, file_id?, file_path?, file_status?, file_error?}`.
3. **Worker integration** — emit events from `process-job!` at key points: job start, file complete, file error, job complete.
4. **Connection management** — clean up disconnected clients, heartbeat every 30s.

## Acceptance Criteria

- [ ] `GET /api/ingestion/jobs/:id/stream` returns `text/event-stream`
- [ ] Events emitted: `job_start`, `file_complete`, `file_error`, `job_complete`, `job_error`
- [ ] Events include `percent_complete` calculated from `processed_files / total_files`
- [ ] Disconnected clients are cleaned up
- [ ] Heartbeat sent every 30s to keep connection alive

## Implementation Notes

- Clojure `manifold` or `core.async` for event bus
- Ring SSE middleware or raw response streaming
- Worker already has progress tracking — just need to add event emission

---

**2026-05-29 — Implemented (status: done).**

SSE-based real-time progress streaming for ingestion jobs.

What was built:

1. **`ingestion/src/kms_ingestion/api/event_bus.clj`** (new) — in-memory event bus.
   No new dependency: a `defonce` atom maps each `job_id` (string) to a set of
   `java.util.concurrent.LinkedBlockingQueue` subscriber queues.
   - `subscribe-to-job!` — registers and returns a fresh queue for a job.
   - `unsubscribe-from-job!` — removes a queue, dropping the job entry when empty.
   - `broadcast-event!` — fan-out an event map to every queue for a job (best-effort,
     never throws, no-op when there are no listeners).
   - `poll-event!` — block up to `timeout-ms` for the next event (returns nil on timeout).
   - `subscriber-count` — listeners for a job.

2. **`ingestion/src/kms_ingestion/api/routes.clj`** — `stream-job-progress-handler`
   wired at `GET /api/ingestion/jobs/:job_id/stream`. Returns `text/event-stream`
   (plus `Cache-Control: no-cache, no-transform`, `Connection: keep-alive`,
   `X-Accel-Buffering: no`) via Ring's `StreamableResponseBody`. Subscribes the client
   *before* the existence check (404s + unsubscribes if the job is unknown), primes the
   stream with a `job_snapshot` of current progress, then drains the queue — writing each
   event as a proper `event:`/`data:` SSE frame. When the queue is idle for 30s it sends a
   `: heartbeat` comment and re-checks job status. The stream closes on a terminal event
   (`job_complete`/`job_error`), on terminal job status, or on client disconnect
   (IOException), always unsubscribing in a `finally`.

3. **`ingestion/src/kms_ingestion/jobs/worker.clj`** — emits events from `process-job!`:
   `job_start` (after total_files known), `file_complete`/`file_error` per file after each
   batch is persisted, and `job_complete`/`job_error` at the end. Every event carries
   `type, job_id, timestamp, total_files, processed_files, failed_files, chunks_created,
   percent_complete` (computed as `100 * processed / total`), with `file_id, file_path,
   file_status, file_error` on per-file events. All emission is best-effort and cannot
   break the worker loop.

4. **`ingestion/test/kms_ingestion/api/event_bus_test.clj`** (new) — 4 tests covering
   subscribe/broadcast/poll roundtrip, poll timeout, multi-subscriber fan-out, and
   unsubscribe cleanup.

Verification (all passing):
- `clj-kondo --lint` on event_bus.clj, routes.clj, worker.clj, event_bus_test.clj → errors: 0, warnings: 0.
- `clojure -M:test` → Ran 76 tests, 309 assertions, 0 failures, 0 errors.
- Namespace compile/load (`require` of event-bus, routes, worker, server) → loaded-ok.
- Live end-to-end Jetty smoke test through the full `wrapped-app` router: `GET .../stream`
  returned HTTP 200 with `Content-Type: text/event-stream; charset=utf-8` and the
  `Cache-Control` header, streamed `job_snapshot` then `job_start`/`file_complete`/
  `job_complete` frames, closed on the terminal event, and unsubscribed the client
  (`subscriber-count` → 0). A 404 case (unknown job) also unsubscribes cleanly.

Acceptance criteria all met: SSE endpoint returns `text/event-stream`; the five event
types are emitted; `percent_complete` is derived from `processed/total`; disconnected
clients are cleaned up; a 30s heartbeat keeps the connection alive.
