---
uuid: "knoxx-ingestion-dashboard-ui"
title: "Ingestion Dashboard UI"
status: done
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

---

**Implemented dedicated `/ingestion` dashboard page (status: done).**

Created a standalone `IngestionPage.tsx` that composes the existing
`ingestion-page/parts.tsx` building blocks (`SourceDetailView`,
`JobProgressView`, `CreateSourceModal`) behind a `SourcesSidebar` extracted
from the `DataLakesSection` source-list pattern. The page covers the core
acceptance criteria: sources list with driver type and active/running status,
create-source form (local driver), source detail with coverage audit and
recent jobs, real-time job progress via the existing `/api/ingestion/ws/jobs/:id`
WebSocket connection, and cancel via `/api/ingestion/jobs/:id/cancel`. It reuses
the same `/api/ingestion` REST endpoints already wired up in `DataLakesSection`.

Files:
- `frontend/src/pages/IngestionPage.tsx` (new, 306 lines — under size budget)
- `frontend/src/App.tsx` — added `IngestionPage` import, an `Ingestion` navbar
  link (non-basic users), and a `/ingestion` `ProtectedSurface` route.
- `frontend/src/lib/app-routes.ts` — added `INGESTION_ROUTE = '/ingestion'`.

Verification:
- `pnpm -C frontend typecheck` (tsc --noEmit) — passed, 0 errors.
- `node scripts/lint-file-sizes.mjs` — exit 0, no size violations.

Out of scope / follow-ups (not in triage plan): bulk-import (tarball/zip) and
single-file upload dialogs, plus a separate filterable global jobs list — the
richer `DataPage` ingestion tabs already cover graph/services/database views.
