# Translation Export Pipeline

Date: 2026-04-06
Status: child spec
Points: 2
Epic: `knowledge-ops-translation-review-epic.md`
Depends on: `knowledge-ops-translation-routes.md`

---

## Purpose

Implement SFT export and manifest endpoints for translation training data.

---

## API Surface

### SFT Export

```
GET /v1/translations/export/sft?project=devel-docs&target_lang=es
```

Returns JSONL with `{prompt, target}` pairs for approved translations.

**Query params:**
- `project` (required): project/tenant ID
- `target_lang`: filter by target language
- `include_corrected`: if true, prefer `corrected_text` over `translated_text` (default: true)

**Response:**
```jsonl
{"prompt": "Translate the following text from English to Spanish. Preserve formatting and technical terms.\n\nText: ...", "target": "..."}
{"prompt": "Translate the following text from English to Spanish. Preserve formatting and technical terms.\n\nText: ...", "target": "..."}
```

If `corrected_text` exists and `include_corrected=true`, uses that as target.
Otherwise uses `translated_text` (MT output).

Only exports segments with status = `approved`.

### Manifest Export

```
GET /v1/translations/export/manifest?project=devel-docs
```

Returns statistics about the translation corpus.

**Query params:**
- `project` (required): project/tenant ID

**Response:**
```json
{
  "project": "devel-docs",
  "generated_at": "2026-04-06T12:00:00Z",
  "languages": {
    "es": {
      "total_segments": 127,
      "approved": 89,
      "rejected": 12,
      "pending": 26,
      "in_review": 0,
      "avg_labels_per_segment": 1.2,
      "with_corrections": 23
    },
    "de": {
      "total_segments": 98,
      "approved": 45,
      "rejected": 8,
      "pending": 40,
      "in_review": 5,
      "avg_labels_per_segment": 1.0,
      "with_corrections": 12
    }
  },
  "labelers": [
    {"email": "translator@example.com", "segments_labeled": 45},
    {"email": "reviewer@example.com", "segments_labeled": 38}
  ],
  "export_sizes": {
    "sft_es": { "rows": 89, "bytes_estimate": 45000 },
    "sft_de": { "rows": 45, "bytes_estimate": 22000 }
  }
}
```

---

## Files to Create/Modify

| File | Purpose |
|------|---------|
| `orgs/open-hax/openplanner/src/routes/v1/translations.ts` | Add `/export/sft`, `/export/manifest` routes |

---

## Implementation Notes

- SFT export streams JSONL directly (don't buffer entire response in memory)
- Manifest computes statistics on-demand from MongoDB aggregation
- Both endpoints require `org.translations.export` permission

---

## Exit Criteria

- [ ] SFT export returns valid JSONL for approved segments
- [ ] SFT export prefers corrected_text when available
- [ ] Manifest returns accurate statistics per language
- [ ] Manifest lists labelers with segment counts
