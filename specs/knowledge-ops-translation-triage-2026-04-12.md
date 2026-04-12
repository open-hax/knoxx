# Knowledge Ops Translation Pipeline Triage

Date: 2026-04-12
Status: canonical triage snapshot
Owner: Knoxx / OpenPlanner

---

## Purpose

Record the canonical translation architecture as it exists today, identify where the implementation and specs disagree, and define the highest-priority repair work.

This document supersedes stale assumptions in older translation specs when they conflict with the live implementation.

---

## Canonical Architecture

The canonical translation pipeline is:

```text
CMS publish to selected garden
  -> OpenPlanner creates translation_jobs per target language
  -> translation-worker consumes queued jobs
  -> translation-worker writes translation_segments
  -> Knoxx /translations review UI loads and labels segments
  -> approved labels/corrections feed SFT export + graph memory
  -> public garden routes may serve translated content when available
```

Key properties:

- Translation is now garden-targeted, not a generic background scan over arbitrary documents.
- Queue truth lives in `translation_jobs`.
- Review truth lives in `translation_segments` and `translation_labels`.
- Knoxx owns the review UI.
- OpenPlanner owns the canonical data and publication state.
- Translation examples for future MT improvement are derived from approved review outcomes.

---

## Canonical Data Model

### OpenPlanner collections

- `translation_jobs`
  - queued translation work items created from CMS/garden publication
- `translation_segments`
  - machine-translated segments awaiting review or already reviewed
- `translation_labels`
  - human review labels and optional corrections
- `events`
  - source documents and CMS publication metadata
- `graph_nodes` / `graph_edges`
  - approved translation examples for MT context enrichment

### Garden publication metadata

Published CMS documents store per-garden publication entries in:

- `extra.metadata.garden_publications`

Each publication entry may include:

- `garden_id`
- `published_at`
- `published_by`
- `translation_status`
- `translated_languages`

This garden-scoped publication metadata is canonical for public document availability.

---

## Live Implementation Map

### OpenPlanner routes implemented

In `src/routes/v1/translations.ts`:

- `GET /v1/translations/segments`
- `GET /v1/translations/segments/:id`
- `POST /v1/translations/segments/:id/labels`
- `POST /v1/translations/segments/batch`
- `GET /v1/translations/export/sft`
- `GET /v1/translations/export/manifest`
- `POST /v1/documents/:id/translate`
- `GET /v1/translations/jobs`
- `GET /v1/translations/examples`
- `GET /v1/translations/graph-stats`

In `src/routes/v1/cms.ts`:

- `POST /v1/cms/publish/:id/:garden_id`
  - validates active garden
  - marks the document public for that garden
  - queues `translation_jobs` for `garden.target_languages`

In `src/routes/v1/public.ts`:

- public garden document responses attempt to serve translated content when available

### Knoxx backend routes implemented

In `packages/knoxx/backend/src/cljs/knoxx/backend/translation_routes.cljs`:

- proxies list/get segment
- proxies label submission
- proxies export endpoints
- proxies batch import

### Knoxx frontend implemented

In `packages/knoxx/frontend/src/pages/TranslationPage.tsx` and related components:

- translation review queue exists at `/translations`
- segment selection exists
- label submission exists
- SFT download exists
- manifest summary exists

### Worker implemented

In `scripts/translation-worker.ts`:

- polls `translation_jobs`
- loads source text from `events`
- chunks text into segments
- calls MT provider via OpenAI-compatible chat completions
- writes `translation_segments`
- marks jobs complete/failed
- queries graph memory for few-shot examples

### Permissions implemented

In `packages/knoxx/backend/src/policy-db.mjs`:

- `org.translations.read`
- `org.translations.review`
- `org.translations.export`
- `org.translations.manage`
- `translator` org role

---

## Live Reality Check

What is working now:

- CMS publish queues translation jobs from garden target languages.
- Translation worker is alive and processing queued jobs.
- Translation routes exist and are test-covered.
- Knoxx translation review page exists and talks to the translation API.
- SFT export and manifest endpoints exist.
- Graph-memory hooks for approved translations exist.

What is directionally correct but not yet trustworthy:

- public garden translation serving
- translation-status writeback to document publication metadata
- manifest statistics
- worker resilience under flaky MT provider conditions
- frontend/backend payload consistency for review UI

---

## Priority-Ordered Gaps

### Priority 0 — correctness blockers

1. Public garden translation lookup does not match live segment shape.
   - `public.ts` queries by `target_language`
   - segments use `target_lang`
   - public lookup expects `garden_id`
   - worker-generated segments do not include `garden_id`

2. Worker does not write completion state back to garden publication metadata.
   - `translation_status` is initialized on publish
   - `translated_languages` is initialized on publish
   - worker never updates them

3. Frontend review list/detail contracts are inconsistent.
   - list UI assumes `segment.labels`
   - list endpoint does not supply labels
   - label timestamp shape is mismatched

### Priority 1 — observability/trust blockers

4. Garden translation counts are wrong or incomplete.
   - current stats count translation segments by `garden_id`
   - worker does not attach `garden_id`

5. Manifest aggregation logic is not aligned with separate labels collection.
   - `with_corrections` currently assumes segment-local labels
   - labeler aggregation does not fully align with actual stored fields

6. Job/segment outcome semantics are too loose.
   - segment-level MT failures become empty/rejected segments
   - a job can still complete despite degraded output quality

### Priority 2 — resilience and quality

7. MT provider failure handling is too brittle.
   - 429/no-key failures
   - malformed or truncated JSON responses
   - no robust retry/backoff stratification

8. Graph-memory enrichment exists in code but is practically dormant.
   - nothing is reliably reaching approved reviewed state yet
   - translation examples remain scarce or zero

---

## Spec Drift Ledger

### Must update immediately

#### `knowledge-ops-translation-routes.md`

Why stale:

- still describes translation segments/labels as event-stored truth
- live implementation uses dedicated Mongo collections
- route and payload assumptions no longer match live data model

Required update:

- rewrite data model section around `translation_segments`, `translation_labels`, and `translation_jobs`
- preserve the API surface, permissions, and overall workflow where still accurate

#### `knowledge-ops-translation-mt-pipeline.md`

Why stale:

- still describes metadata-scanning (`needs_translation`) as the trigger
- live trigger is CMS publish to garden -> queued jobs
- chunking/pipeline details do not match worker implementation

Required update:

- rewrite around queue-driven execution
- document current worker contract and provider behavior
- treat garden target languages as canonical source of job creation

#### `simple-markdown-gardens.md`

Why stale:

- explicitly claims translation complexity was removed
- live garden publication path actively uses translation fields and jobs

Required update:

- restore translation-aware garden semantics
- document `target_languages`, `translation_status`, and `translated_languages` as part of live garden publication behavior

### Should update after contract cleanup

#### `knowledge-ops-translation-review-ui.md`

Why partially stale:

- correct that Knoxx owns the UI
- outdated about payload shape and some UX details
- cleaner than current implementation, but no longer fully descriptive

Required update:

- align component/data contract sections with actual frontend and backend payloads
- preserve the long-term UX intent where still useful

#### `knowledge-ops-translation-review-epic.md`

Why partially stale:

- architecture section still implies older truth boundaries in places
- child-spec decomposition is still useful

Required update:

- mark the queue-based garden-targeted architecture as canonical
- reference this triage doc as the current state baseline

---

## Recommended Repair Order

1. Fix translation/public-serving contract mismatches.
   - align `target_lang` vs `target_language`
   - decide whether `garden_id` belongs on segments or whether public lookup should pivot differently

2. Add worker writeback to publication metadata.
   - update `garden_publications[].translation_status`
   - update `garden_publications[].translated_languages`
   - optionally recalculate garden stats

3. Align frontend review contracts.
   - either include label summaries in list payloads or stop assuming labels on list rows
   - normalize label timestamp fields
   - prefer fetching full segment detail for the active item

4. Repair manifest/stats aggregation.
   - compute correction and labeler stats from `translation_labels`
   - ensure garden translation counts reflect the actual segment/publication relationship

5. Harden worker/provider interaction.
   - better retry/backoff
   - better empty/partial response handling
   - clearer success semantics for partial translation outcomes

6. Update stale specs to match the architecture already in motion.

---

## Canonical Decisions

- The current queue-based garden-targeted translation architecture is canonical.
- Specs that describe event-only translation storage are stale and should not drive new implementation work.
- Specs that claim gardens are translation-free are explicitly wrong relative to live code.
- Review/training and garden publication are part of one system now, but they still need better writeback coupling.

---

## Definition of Done for This Triage

This triage is complete when:

- the canonical architecture is documented in one place
- stale specs are marked for revision or updated
- implementation gaps are prioritized in execution order
- future work is anchored to live system behavior instead of historical design branches
