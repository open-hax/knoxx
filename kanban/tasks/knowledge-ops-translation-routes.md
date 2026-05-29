---
uuid: "knoxx-knowledge-ops-translation-routes"
title: "[LANDED] Translation Routes + Permissions"
status: done
priority: P1
labels: ["tasks", "landed"]
created_at: "2026-04-06T00:00:00Z"
source: "specs/tasks/knowledge-ops-translation-routes.md"
points: 5
category: tasks
---

# Translation Routes + Permissions — LANDED

> Triaged: 2026-05-28
> Implementation: `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs`

## What's Built

All routes from the original spec are implemented and live:

### Segment Routes
- `GET /api/translations/segments` — list with filters (project, status, source_lang, target_lang, domain, limit, offset)
- `GET /api/translations/segments/:id` — get single segment
- `POST /api/translations/segments/batch` — batch create segments

### Label/Review Routes
- `POST /api/translations/segments/:id/labels` — add correction label (with labeler_id, labeler_email, org_id)

### Export Routes
- `GET /api/translations/export/manifest` — export stats by project
- `GET /api/translations/export/sft` — SFT JSONL export (with target_lang, include_corrected filters)

### Document Routes
- `GET /api/translations/documents` — list documents (project, target_lang, source_lang, garden_id)
- `GET /api/translations/documents/:documentId/:targetLang` — get document with segments
- `POST /api/translations/documents/:documentId/:targetLang/review` — review document

### Batch Routes
- `POST /api/translations/batches` — create translation batch
- `GET /api/translations/batches` — list batches (status, garden_id, target_lang)
- `GET /api/translations/batches/next` — get next queued batch for worker
- `GET /api/translations/batches/:id` — get batch detail
- `POST /api/translations/batches/:id/status` — update batch status

### Permissions
- `org.translations.read` — read segments, documents, batches, manifest
- `org.translations.review` — create labels, review documents
- `org.translations.export` — access SFT export
- `org.translations.manage` — create batches, batch create segments, update batch status

### Contracts
- `contracts/capabilities/cap_translation.edn` — capability with save_translation tool
- `contracts/roles/translator.edn` — role with read + review permissions
- `contracts/actors/translations_page.edn` — actor contract
- `contracts/sources/translation_events.edn` — source contract

## Remaining Gaps

None for routes. The spec is fully implemented.

---

Original spec: `specs/tasks/knowledge-ops-translation-routes.md`
