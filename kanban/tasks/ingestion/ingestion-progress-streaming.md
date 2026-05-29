---
uuid: "knoxx-ingestion-progress-streaming"
title: "Ingestion Progress Streaming (WebSocket/SSE)"
status: todo
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
