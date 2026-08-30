---
uuid: "knoxx-knowledge-ops-translation-document-review-v2"
title: "[REGRESSED] Restore document-first translation review over resource CMS"
status: accepted
priority: P0
labels: ["tasks", "regression", "translations", "review", "memory", "frontend", "cms", "has-parent"]
created_at: "2026-04-13T00:00:00Z"
points: null
category: tasks
---

# Restore Translation Document Review v2 over the resource CMS

> Parent epic: `knoxx-evaluation-review-system`
> Reopened: 2026-08-30
> Original implementation landed, then the contract-backed publication path bypassed its
> persisted segments and reduced agent output to one read-only whole-file candidate.
> The original document/segment UI and data shape remain the product target.

## Product decision

The resource CMS and the translation review system are layers, not replacements for one
another:

```text
Document/Garden/Publication resources
        -> desired translation work at an exact source revision
        -> ordered, persisted translation splits and candidate revision
        -> document-first + segment-level human review
        -> approved corrected pairs become translation memory
        -> revision-bound publication approval and reconciliation
```

- The resource graph owns document, garden, locale, source revision, publication intent,
  and whether review is required.
- Persisted translation splits own the source/candidate pairs a person reviews and the
  approved/corrected examples future translation runs learn from.
- Evaluation receipts own immutable judgments and corrections against exact candidate and
  split revisions.
- Publication consumes the effective reviewed candidate. It does not replace splits with a
  whole-file-only approval.

## Regression

The current contract-backed agent sink writes a whole translated file plus a publication
receipt, creates no persisted translation segments, and refuses `segment_index` values other
than `0`. The frontend compensates by splitting text on blank lines into synthetic ids and
then makes those rows read-only because no stored segment exists behind them. This leaves only
`Approve for publication` actionable and makes missing, queued, running, failed, rejected, and
changes-requested resource work invisible.

This is a regression from the workflow below, not a reason to retire it.

## Canonical review shape

Each resource-derived `document + target locale + source revision` has one or more immutable
candidate attempts. A candidate contains an ordered vector of real persisted splits. Each split
retains at least:

- stable split identity and `segment_index`;
- organization/project, garden, document, source locale, and target locale;
- exact source artifact revision and candidate attempt/revision;
- source text, machine candidate text, and content digests;
- current projected status derived from immutable review evidence;
- adequacy, fluency, terminology, risk, overall decision, editor notes, and optional corrected
  target text;
- provenance for the provider/model/configuration that produced it.

Candidate and correction history is append-only. A correction does not destroy the original
machine candidate. The effective target text for an approved split is the approved correction
when present, otherwise the approved machine candidate.

## Translation memory law

- Only approved effective split pairs enter translation memory.
- A corrected approval contributes `source_text -> corrected_text`; the rejected machine output
  remains historical evidence but is never a positive example.
- Rejection contributes negative evaluation evidence and never becomes a positive translation
  example.
- Future translation sessions retrieve approved examples scoped at minimum by organization,
  source locale, and target locale, with garden/domain/terminology relevance used for ranking.
- A new source or candidate revision does not inherit an old approval accidentally.

## Restored review experience

- The left rail lists every resource-derived document/locale work item, including missing,
  queued, running, failed, ready, changes-requested, rejected, approved, and published states.
- Selecting a document opens its complete source/candidate context with real split annotations.
- The review pane exposes adequacy, fluency, terminology, risk, overall, corrected text, and
  editor notes.
- Reviewers can Approve, Needs Edit, Reject, or Skip one split, and use document-level Approve,
  Needs Edit, or Reject as a fast path.
- Dispatch and retry are available from the same workflow; a reviewer does not need another
  operations surface to create the candidate they are expected to review.
- Publication approval is offered only for the exact effective candidate revision produced by
  the completed review projection.

## Existing pieces to preserve

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
- Labels include: adequacy, fluency, terminology, risk, overall, corrected_text
- Reviewer identity tracked (labeler_id, labeler_email)

The newer resource publication contracts, revision-bound translation receipts, dispatch claims,
drift guards, and reconciliation gate are also preserved. The repair composes the two halves;
it does not roll the CMS back.

## Acceptance criteria

1. The work-list projection starts from resource-derived desired translation work, not from
   whichever Mongo segments or completed receipts happen to exist.
2. An agent-produced resource translation persists real ordered splits before its candidate is
   reviewable; no blank-line synthetic ids appear in the write path or UI.
3. The assembled candidate bytes and every split bind to the same organization, document,
   garden, locales, source revision, candidate revision, and producing attempt.
4. The restored three-pane/document-first UI can submit per-split and document-level approve,
   needs-edit, reject, correction, scores, and notes for agent-produced content.
5. A correction remains version-bound history, becomes the effective approved target, and is
   retrieved as translation memory by a later matching translation session.
6. Rejected and stale-revision candidates cannot satisfy publication or enter positive memory.
7. Publication reconciles the reviewed effective document revision, including accepted
   corrections, rather than the uncorrected whole-file output.
8. The old Mongo-derived statistics export is named and presented separately from the resource
   work inventory.
9. Tenant/project scope is server-derived and consistent across inventory, splits, evaluation,
   memory, approval, and publication.
10. A human verification tour seeds multiple documents, dispatches them, corrects one split,
    rejects another candidate, approves a successor, demonstrates memory retrieval, publishes
    the corrected document, and captures the restored UI at each step.
11. Given 18 desired resource-backed document/locale pairs and only one completed candidate, the
    inventory still returns 18 distinct rows: one ready/reviewable row and 17 explicit
    missing/queued/failed rows as their evidence dictates.

## RED-first proof

The first inventory fixture must preserve the observed deployment cardinality before candidate
history can hide it:

```text
18 desired resource document/locale pairs + 1 completed candidate
  -> 18 translation-work inventory rows
  -> no deduplication by whichever receipt/segment collection currently has data
```

The first candidate fixture must then exercise one resource-backed document end to end:

```text
resource intent
  -> dispatch
  -> persisted candidate splits
  -> correction + approval
  -> corrected pair retrievable from translation memory
  -> approval of the effective document revision
  -> reconciliation publishes corrected bytes
```

The fixture is incomplete if it proves only route availability, whole-file approval, or a fake
frontend projection.

## Remaining work

The core review workflow is not live for resource-backed agent translations. The regression is
closed only when all acceptance criteria above pass against the current resource CMS path.

---

The old `specs/` copy was retired when Kanban became source of truth (`2b457af4`).
