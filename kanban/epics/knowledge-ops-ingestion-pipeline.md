---
uuid: "knoxx-knowledge-ops-ingestion-pipeline"
title: "Knowledge Ops — Ingestion Pipeline Spec"
status: breakdown
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.387Z"
source: "specs/epics/knowledge-ops-ingestion-pipeline.md"
points: null
category: epics
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
