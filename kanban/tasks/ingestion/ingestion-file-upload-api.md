---
uuid: "knoxx-ingestion-file-upload-api"
title: "Ingestion File Upload API"
status: todo
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-28T00:00:00Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: 2
category: tasks
---

# Ingestion File Upload API

> Parent: `knowledge-ops-ingestion-pipeline`
> Points: 2

## Purpose

Add a single-file upload endpoint for ingesting individual files without configuring a source.

## Current State

No direct upload endpoint exists. Files must be on a configured source path.

## What Needs Building

1. **Upload endpoint** — `POST /api/ingestion/upload` accepts `multipart/form-data` with a single file.
2. **Storage** — save to a configurable upload directory.
3. **Ingestion** — create a local source pointing at the upload directory, queue a job.
4. **Response** — return `{file_id, job_id}` for tracking.

## Acceptance Criteria

- [ ] `POST /api/ingestion/upload` accepts `multipart/form-data` with `file` field
- [ ] File saved to configurable upload directory
- [ ] Ingestion job created automatically
- [ ] Returns `{file_id, job_id}` for tracking
- [ ] File size limit configurable (default 50MB)
- [ ] Supports common file types (md, txt, pdf, docx)

## Implementation Notes

- Ring middleware for multipart parsing
- Reuse local driver for processing
- Upload directory configurable via `UPLOAD_DIR` env var
