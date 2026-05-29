---
uuid: "knoxx-knowledge-ops-translation-document-review-v2"
title: "[LANDED] Translation Document Review v2"
status: done
priority: P2
labels: ["tasks", "landed"]
created_at: "2026-04-13T00:00:00Z"
source: "specs/tasks/knowledge-ops-translation-document-review-v2.md"
points: null
category: tasks
---

# Translation Document Review v2 — LANDED

> Triaged: 2026-05-28
> Supersedes: `knowledge-ops-translation-review-ui.md`

## What's Built

### Backend
- `GET /api/translations/documents` — list documents with filters
- `GET /api/translations/documents/:documentId/:targetLang` — get document with embedded segments
- `POST /api/translations/documents/:documentId/:targetLang/review` — submit review

### Frontend
- `TranslationSegmentList.tsx` — segment list with status badges (approved, rejected, in_review, pending)
- `TranslationReviewCard.tsx` — review form with adequacy, fluency, terminology, risk, overall ratings
- `TranslationManifestCard.tsx` — export stats display

### Review Model
- Segments are annotations within documents (not flat queue)
- Labels include: adequacy, fluency, terminology, risk, overall, correction_text
- Reviewer identity tracked (labeler_id, labeler_email)

## Remaining Gaps

None for the core review workflow. Document-based review is live.

---

Original spec: `specs/tasks/knowledge-ops-translation-document-review-v2.md`
