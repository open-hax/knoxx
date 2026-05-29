---
uuid: "knoxx-knowledge-ops-translation-mt-pipeline"
title: "[LANDED] Translation MT Pipeline"
status: done
priority: P1
labels: ["tasks", "landed"]
created_at: "2026-04-06T00:00:00Z"
source: "specs/tasks/knowledge-ops-translation-mt-pipeline.md"
points: 3
category: tasks
---

# Translation MT Pipeline — LANDED

> Triaged: 2026-05-28
> Implementation: `ingestion/src/kms_ingestion/translation/worker.clj`

## What's Built

The MT pipeline is implemented as a Clojure ingestion worker:

### Worker Architecture
- Polls `GET /api/translations/batches/next` for queued batches
- Batch model: one garden + one target language + N published documents
- Uses knoxx agent runtime for translation (cross-document context for terminology consistency)
- Configurable poll interval (default 10s)
- Translation model cached with TTL

### Batch Lifecycle
```
CMS publish → translation_batches created (queued)
  → worker picks up batch (in_progress)
  → agent session translates all documents
  → segments written to OpenPlanner
  → batch marked completed/failed
```

### Key Properties
- Garden-targeted (not generic background scan)
- Queue truth: `translation_batches` in OpenPlanner
- Worker talks to both OpenPlanner (data) and knoxx-backend (agent runtime)
- Auth via API keys + x-knoxx-user-email header

## Remaining Gaps

None for the core pipeline. Worker is live and processing batches.

---

Original spec: `specs/tasks/knowledge-ops-translation-mt-pipeline.md`
Historical triage: `specs/archived/reference/knowledge-ops-translation-triage-2026-04-12.md`
