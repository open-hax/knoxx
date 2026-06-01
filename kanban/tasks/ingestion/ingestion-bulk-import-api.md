---
uuid: "knoxx-ingestion-bulk-import-api"
title: "Ingestion Bulk Import API"
status: done
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

---

**Status: done** — Bulk import endpoint implemented end-to-end.

What was built:

- Added `org.apache.commons/commons-compress {:mvn/version "1.26.0"}` to `ingestion/deps.edn` for archive extraction.
- New namespace `kms_ingestion/api/bulk_import.clj`:
  - `archive-format` detects `.tar.gz` / `.tgz` / `.tar` / `.zip` by filename, returns nil for unsupported types.
  - `extract-archive!` unpacks tar/tar.gz/zip via commons-compress with a zip-slip guard (`normalized-target` rejects any entry that escapes the destination root).
  - `max-upload-bytes` defaults to 100MB, overridable via `KNOXX_BULK_IMPORT_MAX_BYTES`.
  - `bulk-import-handler` reads the multipart `file` field, validates format + size (returns 400 for missing/unsupported file, 413 for oversize), creates a temp dir, extracts the archive, registers a temporary `local` source (config carries a `:bulk_import true` marker + `:temp_path`), creates and queues a job, and returns `{job_id, source_id, tenant_id, temp_path, file_count, status: "queued"}`. On extraction failure the temp dir is reaped and 400 is returned.
- `jobs/worker.clj`: added `cleanup-temp-source!` (best-effort, marker-driven via `:bulk_import` config flag) and invoke it at job completion; added `babashka.fs` require.
- `api/routes.clj`: required `bulk-import` ns and added `["/bulk" {:post bulk-import/bulk-import-handler}]` under `/api/ingestion`.
- New test ns `test/kms_ingestion/api/bulk_import_test.clj` covering format detection, real tar.gz extraction, zip-slip rejection, missing-file/unsupported/oversize handler paths, the full extract+queue happy path (DB + worker redefed), and temp cleanup (marked vs unmarked).

Verification (all clean):

- `clj-kondo` on changed files: errors 0, warnings 0.
- `clojure -M:test`: Ran 54 tests, 217 assertions, 0 failures, 0 errors.
- `clojure -M:uberjar`: packaged `target/kms-ingestion.jar` (23.8MB); commons-compress classes confirmed bundled.

Note: `api/routes.clj` is 534 lines (over the 500-line repo budget) but was already 530 before this card; the new handler logic was deliberately placed in `bulk_import.clj` (161 lines) to avoid growing routes.clj further. Reducing routes.clj below budget is out of scope for this card.
