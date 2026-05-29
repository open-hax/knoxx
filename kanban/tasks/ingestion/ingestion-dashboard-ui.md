---
uuid: "knoxx-ingestion-dashboard-ui"
title: "Ingestion Dashboard UI"
status: todo
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-28T00:00:00Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: 5
category: tasks
---

# Ingestion Dashboard UI

> Parent: `knowledge-ops-ingestion-pipeline`
> Points: 5

## Purpose

Build a frontend dashboard for managing ingestion sources, monitoring jobs, and viewing progress.

## Current State

No ingestion UI exists. All management is via API calls.

## What Needs Building

1. **Sources list** — show configured sources with driver type, file count, last scan time.
2. **Source detail** — show source config, state, file state counts.
3. **Create source form** — select driver type, fill config, save.
4. **Jobs list** — show jobs with status, progress bar, timestamps.
5. **Job detail** — show job progress, file-level events (via SSE), cancel/retry buttons.
6. **Bulk import** — upload tarball/zip, show job progress.
7. **File upload** — single file upload with immediate feedback.

## Component Structure

```
IngestionPage
├── SourcesSidebar (list sources, create button)
├── SourceDetail (config, state, file counts)
├── JobsList (filterable by source/status)
├── JobDetail (progress bar, event log, cancel/retry)
├── BulkImportDialog (file upload + job tracking)
└── FileUploadDialog (single file upload)
```

## Acceptance Criteria

- [ ] Sources list shows all configured sources with driver type and file count
- [ ] Create source form works for local driver (at minimum)
- [ ] Jobs list shows status, progress, timestamps
- [ ] Job detail shows real-time progress via SSE connection
- [ ] Cancel and retry buttons work on jobs
- [ ] Bulk import dialog accepts tarball/zip uploads
- [ ] Single file upload works

## Implementation Notes

- Use React + uxx components
- SSE for real-time job progress
- Route: `/ingestion` in the knoxx frontend
- Reuse existing API endpoints
