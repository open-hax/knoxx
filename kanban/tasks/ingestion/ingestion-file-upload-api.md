---
uuid: "knoxx-ingestion-file-upload-api"
title: "Ingestion File Upload API"
status: done
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

---

**Implemented (2026-05-29).** Added a single-file upload endpoint following the existing bulk-import pattern.

What was built:
- `ingestion/src/kms_ingestion/config.clj` — added `upload-dir` (env `UPLOAD_DIR`, default `/tmp/kms-uploads`) and `upload-max-bytes` (env `UPLOAD_MAX_BYTES`, default 50MB) config keys + accessors.
- `ingestion/src/kms_ingestion/api/file_upload.clj` (new, 113 lines) — `file-upload-handler` parses the `file` multipart field, validates extension (`.md/.txt/.pdf/.docx` via `allowed-extension?`), enforces the size limit (413), saves the upload to the upload dir under a UUID-based filename (preserving extension), creates a `local` source scoped to the saved file via `:include-patterns`, queues a job, and returns `{file_id, job_id, source_id, filename, status}`. Cleans up the saved file on queue failure.
- `ingestion/src/kms_ingestion/api/routes.clj` — required `file-upload` ns and registered `["/upload" {:post file-upload/file-upload-handler}]` under `/api/ingestion`.
- `ingestion/test/kms_ingestion/api/routes_test.clj` — added 5 tests covering allowed/unsupported file types, missing `file` field (400), size limit (413), and the happy path (file saved on disk, local source created at upload dir, job queued, `{file_id, job_id}` response format) with db/worker stubbed via `with-redefs`.

Verification:
- `clj-kondo --lint` on changed files: errors 0, warnings 0.
- `clojure -M:test`: Ran 59 tests containing 244 assertions. 0 failures, 0 errors.
