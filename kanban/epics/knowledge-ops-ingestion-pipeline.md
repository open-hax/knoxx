---
uuid: "knoxx-knowledge-ops-ingestion-pipeline"
title: "Knowledge Ops — Ingestion Pipeline Spec"
status: "ready"
priority: "P2"
labels: ["epics"]
created_at: "2026-05-28T22:40:14.387Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: null
category: "epics"
---

# Knowledge Ops — Ingestion Pipeline Spec

> Source: `specs/epics/knowledge-ops-ingestion-pipeline.md`

> *The driver is the gateway. The queue is the memory. The stream is the pulse.*

---
## Purpose

Define a driver-based ingestion system that can import knowledge from multiple sources (local filesystem, cloud storage, code repos, ticketing systems) with state tracking, progress streaming, and resume capability.
---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    INGESTION PIPELINE                            │

---
## Triage Notes (2026-05-28)

- **Status**: Keep as breakdown. Large 888-line spec, comprehensive architecture.
- **Action needed**: Split into child tasks (est. 5-8 tasks, each ≤5sp).
- **Key deliverables**:
  - Driver interface + registry (local filesystem, cloud, git, tickets)
  - Queue state machine (queued → processing → complete/failed)
  - Progress streaming API
  - Resume capability (checkpoint per driver)
  - Content normalization pipeline
  - Error handling + dead-letter queue
- **Assessment**: This is foundational infrastructure. Prioritize based on which drivers are actually needed first (likely filesystem + git).
---

## Triage Notes (2026-05-28)

### What's Already Built (Landed)

The core ingestion pipeline is implemented in Clojure (`kms-ingestion` package):

| Component | File | Status |
|-----------|------|--------|
| Driver protocol | `drivers/protocol.clj` | Landed |
| Driver registry | `drivers/registry.clj` | Landed (7 drivers) |
| Local driver | `drivers/local.clj` | Landed |
| Audio driver | `drivers/audio.clj` | Landed (AI-powered) |
| Image driver | `drivers/image.clj` | Landed (AI-powered) |
| Scraper driver | `drivers/scraper.clj` | Landed |
| Eta-mu sessions driver | `drivers/eta_mu_sessions.clj` | Landed |
| OpenCode sessions driver | `drivers/opencode_sessions.clj` | Landed |
| PromptDB driver | `drivers/promptdb.clj` | Landed |
| Job worker | `jobs/worker.clj` | Landed |
| Job control | `jobs/control.clj` | Landed |
| DB schema | `db.clj` | Landed (sources, jobs, file_state tables) |
| API routes | `api/routes.clj` | Landed (sources CRUD, jobs CRUD, drivers list) |
| Graph integration | `graph.clj` | Landed |
| Translation worker | `translation/worker.clj` | Landed |

### What's NOT Built (Child Tasks)

| Task | Points | Priority |
|------|--------|----------|
| Progress streaming (WebSocket/SSE) | 3 | P2 |
| GitHub driver | 5 | P2 |
| Google Drive driver | 5 | P3 (icebox) |
| Bulk import API | 3 | P2 |
| File upload API | 2 | P2 |
| Dashboard UI | 5 | P2 |

**Total remaining: 23 points across 6 tasks.**

### Spec vs Reality

The spec describes a Python/FastAPI implementation. The actual implementation is Clojure/Reitit. The architecture (driver protocol, job queue, file state tracking) matches the spec, but the language and specific frameworks differ. The spec's Python code should be read as design intent, not implementation reference.

---
Breakdown 2026-05-29: All 6 child deliverables confirmed done in both kanban and codebase. GitHub driver (drivers/github.clj, 462 lines, full protocol impl), SSE progress streaming (api/event_bus.clj + routes.clj stream-job-progress-handler), bulk import API (api/bulk_import.clj), file upload API (api/file_upload.clj), and dashboard UI (frontend/src/pages/IngestionPage.tsx) are all landed. Google Drive driver is icebox at P3 as intended. The 2026-05-28 triage note 'not built' list is entirely superseded — all items were completed as tracked child tasks (knoxx-ingestion-github-driver, knoxx-ingestion-progress-streaming, knoxx-ingestion-bulk-import-api, knoxx-ingestion-file-upload-api, knoxx-ingestion-dashboard-ui — all status: done). Story points: 1sp (close-out only). Verdict: ready. DoD: run eta-mu-beta kanban frontmatter knoxx-knowledge-ops-ingestion-pipeline status done — no remaining work. --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban

Breakdown 2026-05-29: All 6 child deliverables confirmed done in both kanban and codebase. GitHub driver (ingestion/src/kms_ingestion/drivers/github.clj, 462 lines, full Driver protocol impl with rate-limit backoff), SSE progress streaming (api/event_bus.clj + routes.clj stream-job-progress-handler at GET /api/ingestion/jobs/:job_id/stream), bulk import API (api/bulk_import.clj, tar/zip extraction with zip-slip guard), file upload API (api/file_upload.clj), and ingestion dashboard UI (frontend/src/pages/IngestionPage.tsx) are all landed. Google Drive driver is intentionally icebox at P3. The 2026-05-28 triage note 'not built' list is entirely superseded — all items were completed as tracked child tasks (knoxx-ingestion-github-driver, knoxx-ingestion-progress-streaming, knoxx-ingestion-bulk-import-api, knoxx-ingestion-file-upload-api, knoxx-ingestion-dashboard-ui — all status: done on the board). Story points: 1sp (close-out only). Verdict: ready. DoD: run `eta-mu-beta kanban frontmatter knoxx-knowledge-ops-ingestion-pipeline status done` — no implementation work remains. --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
