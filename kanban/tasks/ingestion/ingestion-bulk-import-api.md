---
uuid: "knoxx-ingestion-bulk-import-api"
title: "Ingestion Bulk Import API"
status: todo
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-28T00:00:00Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: 3
category: tasks
---

# Ingestion Bulk Import API

> Parent: `knowledge-ops-ingestion-pipeline`
> Points: 3

## Purpose

Add a bulk import endpoint that can ingest a tarball/zip of files in a single request.

## Current State

The existing `create-job-handler` creates a job for a configured source. There's no way to upload a batch of files directly without configuring a source first.

## What Needs Building

1. **Upload endpoint** — `POST /api/ingestion/bulk` accepts `multipart/form-data` with a tar.gz or zip file.
2. **Extraction** — unpack archive to a temp directory, create a temporary local source.
3. **Job creation** — create an ingestion job for the temp source.
4. **Cleanup** — delete temp directory after job completes.
5. **Response** — return job ID for progress tracking.

## Acceptance Criteria

- [ ] `POST /api/ingestion/bulk` accepts `multipart/form-data` with `file` field
- [ ] Supports `.tar.gz`, `.zip`, `.tar` formats
- [ ] Creates temporary local source + ingestion job
- [ ] Returns `{job_id, source_id}` for tracking
- [ ] Temp files cleaned up after job completes
- [ ] File size limit configurable (default 100MB)

## Implementation Notes

- Use `clojure.java.io` for file I/O
- `babashka.fs` for temp directory management
- Reuse existing local driver for processing
- Archive extraction via `commons-compress` or shell out to `tar`/`unzip`
