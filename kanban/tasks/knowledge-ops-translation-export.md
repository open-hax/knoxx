---
uuid: "knoxx-knowledge-ops-translation-export"
title: "[LANDED] Translation Export Pipeline"
status: done
priority: P2
labels: ["tasks", "landed"]
created_at: "2026-04-06T00:00:00Z"
source: "specs/tasks/knowledge-ops-translation-export.md"
points: 2
category: tasks
---

# Translation Export Pipeline — LANDED

> Triaged: 2026-05-28
> Implementation: routes in `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs`, client in `infra/clients/openplanner.cljs`

## What's Built

### SFT Export
- `GET /api/translations/export/sft` — returns NDJSON of approved corrections
- Filters: `project`, `target_lang`, `include_corrected`
- Content-Type: `application/x-ndjson`

### Manifest
- `GET /api/translations/export/manifest` — returns stats by project
- Shows language pair counts and segment status breakdown

### Frontend
- `TranslationManifestCard.tsx` — displays export stats in the UI

## Remaining Gaps

None for export. Routes and frontend are live.

---

Original spec: `specs/tasks/knowledge-ops-translation-export.md`
