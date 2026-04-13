# Translation Document Review v2

Date: 2026-04-13
Status: active
Epic: knowledge-ops-translation-review-epic
Supersedes: knowledge-ops-translation-review-ui.md
Depends on: knowledge-ops-translation-routes.md

---

## Purpose

Redesign the translation review interface around the document as the primary review unit.
Segments remain the storage model but become annotations within the document view —
not items in a flat queue.

Also redesign the translation worker to batch all documents for a garden+target_lang
into a single agent session, so the translator carries context across documents for
consistent terminology and style.

---

## Core Principles

1. **Document-first review.** The left rail lists translated documents. Selecting one
   opens the full document with inline segment annotations. Reviewers work on documents,
   not on a segment queue.

2. **Batch translation sessions.** Instead of one job per document, one agent session
   translates all published documents in a garden for a target language. This gives the
   agent cross-document context for terminology and style consistency.

3. **Annotation model for segments.** Segment boundaries are visual overlays — like
   comments in Google Docs. Each segment shows its status badge, review summary, and can
   be expanded for full label/correction editing. The reviewer sees the whole document,
   not isolated snippets.

4. **Dual-level review.** Reviewers can:
   - Approve/reject at the document level (fast path for good translations)
   - Drill into specific segments for correction (when only part of the doc needs work)

---

## Batch Worker Design

### Current (v1)
- One `translation_job` per document per target language
- Worker polls for next job, starts one agent session per job
- Each session translates one document in isolation
- No cross-document terminology context

### New (v2)
- One `translation_batch` per garden per target language
- Batch contains all published documents that need translation
- Worker starts one agent session per batch
- Agent session receives all document content, translates all in sequence
- Consistent terminology across documents within the same batch session

### Batch Schema (MongoDB: `translation_batches`)

```ts
interface TranslationBatch {
  _id: ObjectId;
  batch_id: string;           // UUID
  garden_id: string;
  target_lang: string;
  source_lang: string;        // default "en"
  project: string;
  status: "queued" | "processing" | "complete" | "partial" | "failed";
  document_ids: string[];     // all docs in the batch
  completed_documents: string[];  // docs where segments were saved
  failed_documents: { document_id: string; error: string }[];
  agent_session_id?: string;
  agent_conversation_id?: string;
  agent_run_id?: string;
  created_at: Date;
  started_at?: Date;
  completed_at?: Date;
  error?: string;
}
```

### Agent Session Prompt

```
You are the Knoxx translator agent. Translate all supplied documents from {source_lang}
to {target_lang}.

Rules:
1. Preserve meaning, tone, markdown structure, links, and list structure.
2. For each document, split into logical segments and call save_translation for every
   translated segment.
3. Every save_translation call must include: source_text, translated_text, source_lang,
   target_lang, document_id, garden_id, project, segment_index.
4. Set project to '{project}' and garden_id to '{garden_id}'.
5. translated_text must be in {target_lang}; do not copy source text for prose.
6. Maintain consistent terminology across all documents within this batch.
7. After completing all documents, provide a brief summary of translations done.

Documents to translate:
{document_inventory}
```

The message body includes full content for each document, prefixed with a clear
document header so the agent can distinguish boundaries.

### Worker Flow (v2)

1. Poll for next batch (`GET /v1/translations/batches/next`)
2. Mark batch as `processing`
3. Fetch all document content for the batch
4. Build batch prompt with document inventory + all content
5. Call `/api/knoxx/direct/start` with translator agent_spec
6. Poll for segment creation per document
7. Mark batch as `complete` (all docs) or `partial` (some failed)

---

## Review UI Design

### Route

`/ops/translations` — Translation Review (document-first)

Sub-routes:
- `/ops/translations` — document list view (default)
- `/ops/translations/:documentId/:targetLang` — document annotation view

### Layout: Document List View

```
┌───────────────────────────────────────────────────────────────────┐
│ Translation Review                                    [SFT Export]│
├───────────────────────────────────────────────────────────────────┤
│ Filters: [Project ▾] [Target Lang ▾] [Status ▾]                 │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ 📄 Submodule CLI Guide                              en → ko │  │
│  │    knoxx-docs garden  •  15 segments  •  2 approved         │  │
│  │    ⚠️ 13 pending review                              [Open] │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ 📄 API Reference                                    en → es │  │
│  │    knoxx-docs garden  •  8 segments  •  8 approved          │  │
│  │    ✅ Fully reviewed                                 [Open] │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ 📄 Deployment Guide                                 en → de │  │
│  │    knoxx-docs garden  •  12 segments  •  0 reviewed         │  │
│  │    🔴 12 pending                                    [Open] │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

Each card represents a unique document+target_lang combination.
Card shows: title, language pair, garden, segment count, approval progress, status.

### Layout: Document Annotation View

```
┌──────────────┬──────────────────────────────────────────────────┐
│ Segments     │ Document: Submodule CLI Guide                    │
│ (annotations)│ en → ko  •  knoxx-docs garden                    │
│              │                                                   │
│ ┌──────────┐ │  # Submodule CLI Guide                           │
│ │ 1 ✅     │ │  ▸ Submodule CLI is a unified command-line       │
│ │ approved │ │    interface for managing git submodules.        │
│ └──────────┘ │  ◀── segment 0 (approved) ──▶                   │
│ ┌──────────┐ │                                                   │
│ │ 2 ⚠️     │ │  ## Migration Mapping                            │
│ │ pending  │ │  ▸ | Old Script | New Command | Notes |          │
│ └──────────┘ │    | bin/sync   | sub sync     | legacy  |       │
│ ┌──────────┐ │  ◀── segment 1 (pending review) ──▶             │
│ │ 3 ⚠️     │ │                                                   │
│ │ pending  │ │  ## Features                                     │
│ └──────────┘ │  ▸ Enhanced help system with per-command docs    │
│              │    and comprehensive option descriptions.         │
│ ┌──────────┐ │  ◀── segment 2 (pending review) ──▶             │
│ │ 4 ✅     │ │                                                   │
│ │ approved │ │  ## Implementation                               │
│ └──────────┘ │  ▸ Technical stack details and architecture      │
│              │    notes for the module system.                   │
│              │  ◀── segment 3 (approved) ──▶                    │
│              │                                                   │
│              │  ## Future Improvements                           │
│              │  ▸ Interactive mode for complex tasks and         │
│              │    progress bars for long-running operations.     │
│              │  ◀── segment 4 (pending review) ──▶              │
│              │                                                   │
│              ├──────────────────────────────────────────────────┤
│              │ [Approve All] [Approve Reviewed] [Reject All]    │
│              │ Document-level: [Approve] [Needs Edit] [Reject]  │
│              ├──────────────────────────────────────────────────┤
│              │ Segment Detail (expanded annotation)              │
│              │ ┌─────────────────┬─────────────────┐            │
│              │ │ Source (en)     │ Translation (ko) │            │
│              │ │ ## Migration    │ ## 마이그레이션   │            │
│              │ │ Mapping...      │ 매핑...          │            │
│              │ └─────────────────┴─────────────────┘            │
│              │ Adequacy [▾] Fluency [▾] Terminology [▾]         │
│              │ Risk [▾]   Corrected: [___________]              │
│              │ Notes: [____________]                             │
│              │ [Approve] [Needs Edit] [Reject]                  │
│              └──────────────────────────────────────────────────┘
└──────────────┴──────────────────────────────────────────────────┘
```

### Annotation Model

Segments are rendered as inline regions within the document. Each region has:

- **Status badge**: colored indicator (✅ approved, ⚠️ pending, 🔴 rejected, 📝 in_review)
- **Boundary markers**: subtle visual indicators where segments begin and end
- **Click to expand**: clicking a segment opens the detail panel below the document
- **Hover preview**: hovering shows a tooltip with the translated text preview

The document is rendered from the **source text** with segment boundaries overlaid.
The translated text appears in the detail panel when a segment is selected.

### Segment Boundary Rendering

Segments are defined by `segment_index` and `source_text`. To render boundaries:

1. Fetch all segments for the document+target_lang
2. Sort by `segment_index`
3. Concatenate `source_text` fields to reconstruct the source document
4. Render each segment as a `<div>` with:
   - `data-segment-index` attribute
   - `data-segment-status` attribute
   - CSS class for status-based styling
   - Click handler to select the segment

---

## API Changes

### New: List Translated Documents

```
GET /v1/translations/documents?project=devel&target_lang=ko
```

Returns a list of unique document+target_lang combinations with aggregated stats:

```json
{
  "documents": [
    {
      "document_id": "477d7a2d-...",
      "target_lang": "ko",
      "source_lang": "en",
      "garden_id": "knoxx-docs",
      "project": "devel",
      "title": "Submodule CLI Guide",
      "total_segments": 15,
      "approved": 2,
      "pending": 13,
      "rejected": 0,
      "in_review": 0,
      "overall_status": "pending_review",
      "document_status": "public"
    }
  ],
  "total": 3
}
```

This is an aggregation over `translation_segments` grouped by `(document_id, target_lang)`,
joined with document metadata from the events collection.

### New: Get Document Translation

```
GET /v1/translations/documents/:documentId/:targetLang
```

Returns the full source document content plus all segments for that document+lang pair:

```json
{
  "document": {
    "id": "477d7a2d-...",
    "title": "Submodule CLI Guide",
    "content": "# Submodule CLI Guide\n\n...",
    "source_lang": "en",
    "visibility": "public"
  },
  "segments": [
    {
      "id": "...",
      "source_text": "...",
      "translated_text": "...",
      "segment_index": 0,
      "status": "approved",
      "labels": [...]
    }
  ],
  "summary": {
    "total_segments": 15,
    "approved": 2,
    "pending": 13,
    "overall_status": "pending_review"
  }
}
```

### New: Document-Level Review Action

```
POST /v1/translations/documents/:documentId/:targetLang/review
```

Body:
```json
{
  "overall": "approve" | "needs_edit" | "reject",
  "editor_notes": "string",
  "segment_overrides": {
    "3": { "overall": "reject", "corrected_text": "..." },
    "7": { "overall": "needs_edit" }
  }
}
```

Applies the overall review to all segments in the doc+lang, with optional per-segment
overrides. This is the fast path: approve all at once, reject specific bad ones.

### New: Batch Endpoints

```
POST /v1/translations/batches
GET  /v1/translations/batches
GET  /v1/translations/batches/next
POST /v1/translations/batches/:id/status
GET  /v1/translations/batches/:id
```

### Modified: CMS Publish Creates Batches

When `POST /cms/publish/:id/:garden_id` queues translation, instead of creating
individual `translation_jobs`, it creates or appends to a `translation_batch` for
the garden+target_lang.

---

## Frontend Components

### New Files

| File | Purpose |
|------|---------|
| `TranslationReviewPage.tsx` | Document-first review page (replaces TranslationPage.tsx) |
| `TranslationDocumentList.tsx` | Left rail: document cards with status summary |
| `TranslationDocumentAnnotation.tsx` | Document content with inline segment annotations |
| `TranslationSegmentAnnotation.tsx` | Single inline segment boundary + status badge |
| `TranslationSegmentDetail.tsx` | Expanded segment detail panel (source/translation/labels) |
| `TranslationDocumentActions.tsx` | Document-level approve/reject buttons |

### Removed/Deprecated Files

| File | Reason |
|------|--------|
| `TranslationPage.tsx` | Replaced by `TranslationReviewPage.tsx` |
| `TranslationSegmentList.tsx` | Replaced by `TranslationDocumentList.tsx` |
| `TranslationReviewCard.tsx` | Replaced by `TranslationSegmentDetail.tsx` |

`TranslationManifestCard.tsx` is kept but moved into the page header as a compact stats bar.

---

## Exit Criteria

- [ ] `/ops/translations` shows document list with aggregated segment stats
- [ ] Selecting a document opens annotation view with inline segment boundaries
- [ ] Clicking a segment expands detail panel with source/translation/labels
- [ ] Document-level approve/reject works (applies to all segments)
- [ ] Per-segment overrides work within document review
- [ ] Translation worker batches by garden+target_lang
- [ ] Batch status visible in ops/agents history
- [ ] SFT export still works with same data
- [ ] Existing segment-level API still works (backward compat)

---

## Migration Notes

- Existing `translation_jobs` continue to work; new batches are separate collection
- Old `TranslationPage.tsx` can be kept at a legacy route if needed
- `project: null` segments should be backfilled before launch:
  ```js
  db.translation_segments.updateMany(
    { project: null },
    [{ $set: { project: "devel" } }]
  )
  ```
