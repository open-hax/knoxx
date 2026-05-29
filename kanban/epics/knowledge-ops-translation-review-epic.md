---
uuid: "knoxx-knowledge-ops-translation-review-epic"
title: "[LANDED] Knowledge Ops — Translation Review Epic"
status: done
priority: P2
labels: ["epics", "landed"]
created_at: "2026-04-06T00:00:00Z"
source: "specs/epics/knowledge-ops-translation-review-epic.md"
points: null
category: epics
---

# Knowledge Ops — Translation Review Epic — LANDED

> Triaged: 2026-05-28
> All child tasks landed. Epic complete.

## Status

| Child Spec | Points | Status |
|------------|--------|--------|
| translation-routes | 5 | LANDED — all routes + permissions implemented |
| translation-mt-pipeline | 3 | LANDED — worker.clj processing batches |
| translation-export | 2 | LANDED — SFT export + manifest live |
| translation-document-review-v2 | — | LANDED — document-based review UI live |

**Total: 10 points, all landed.**

## What's Live

- **Routes**: 15+ endpoints (segments, labels, documents, batches, export)
- **Worker**: Clojure ingestion worker polls for batches, uses knoxx agent runtime
- **Frontend**: TranslationSegmentList, TranslationReviewCard, TranslationManifestCard
- **Contracts**: cap_translation, translator role, translations_page actor
- **Permissions**: org.translations.read, .review, .export, .manage

## Canonical Architecture

```
CMS publish → translation_batches (queued)
  → worker.clj picks up batch (in_progress)
  → knoxx agent session translates documents
  → segments written to OpenPlanner
  → Knoxx /translations review UI loads documents + segments
  → reviewers label corrections (adequacy, fluency, terminology, risk)
  → approved labels feed SFT export
  → public garden serves translated content
```

---

Original spec: `specs/epics/knowledge-ops-translation-review-epic.md`
