# Translation Routes + Permissions

Date: 2026-04-06
Status: child spec
Points: 5
Epic: `knowledge-ops-translation-review-epic.md`
Depends on: OpenPlanner MongoDB migration complete

---

## Purpose

Implement translation segment CRUD routes in OpenPlanner and add translation permissions to Knoxx policy-db.

This is the critical-path foundation — all other translation work depends on this.

---

## Data Models

### Translation Segment Event

Translation segments are stored as OpenPlanner events with kind `translation.segment`:

```typescript
type TranslationSegmentEvent = {
  schema: "openplanner.event.v1";
  id: string;                    // UUID
  ts: string;                    // ISO timestamp
  source: "mt" | "human" | "import";
  kind: "translation.segment";

  source_ref: {
    project: string;             // e.g., "devel-docs"
    document_id: string;         // source document
    segment_index: number;       // position in source
  };

  text: string;                  // translated text (MT output or human)

  meta: {
    source_lang: string;         // e.g., "en"
    target_lang: string;         // e.g., "es"
    source_text: string;         // original text
    mt_model?: string;           // e.g., "glm-5"
    confidence?: number;         // 0-1 MT confidence if available
    status: "pending" | "in_review" | "approved" | "rejected";
  };

  extra: {
    tenant_id: string;
    org_id: string;
    domain?: string;             // e.g., "support", "legal", "marketing"
    content_type?: string;       // e.g., "faq", "policy", "product"
    url_context?: string;        // source URL if web content
  };
};
```

### Label Event

Labels are stored as separate events with kind `translation.label`:

```typescript
type TranslationLabelEvent = {
  schema: "openplanner.event.v1";
  id: string;                    // UUID
  ts: string;                    // ISO timestamp
  source: "shibboleth";          // always shibboleth for labels
  kind: "translation.label";

  source_ref: {
    project: string;
    segment_id: string;          // references translation.segment event
    document_id: string;
  };

  meta: {
    labeler_id: string;          // user ID from Knoxx auth
    labeler_email: string;
    label_version: number;       // increment on re-label
  };

  extra: {
    tenant_id: string;
    org_id: string;

    // Label dimensions
    adequacy: "excellent" | "good" | "adequate" | "poor" | "unusable";
    fluency: "excellent" | "good" | "adequate" | "poor" | "unusable";
    terminology: "correct" | "minor_errors" | "major_errors";
    risk: "safe" | "sensitive" | "policy_violation";

    // Overall
    overall: "approve" | "needs_edit" | "reject";

    // Correction
    corrected_text?: string;     // human-corrected translation
    editor_notes?: string;
  };
};
```

---

## API Surface

### List Translation Segments

```
GET /v1/translations/segments
```

Query params:
- `project` (required): tenant/project ID
- `status`: filter by status (`pending`, `in_review`, `approved`, `rejected`)
- `source_lang`: filter by source language
- `target_lang`: filter by target language
- `domain`: filter by domain
- `limit`: max results (default 50)
- `offset`: pagination offset

Response:
```json
{
  "segments": [
    {
      "id": "uuid",
      "source_text": "...",
      "translated_text": "...",
      "source_lang": "en",
      "target_lang": "es",
      "status": "pending",
      "confidence": 0.87,
      "mt_model": "glm-5",
      "document_id": "...",
      "segment_index": 0,
      "domain": "support",
      "labels": []
    }
  ],
  "total": 127,
  "has_more": true
}
```

### Get Single Segment

```
GET /v1/translations/segments/:id
```

Response includes the segment plus all labels (most recent first).

### Submit Label

```
POST /v1/translations/segments/:id/labels
```

Body:
```json
{
  "adequacy": "good",
  "fluency": "excellent",
  "terminology": "correct",
  "risk": "safe",
  "overall": "approve",
  "corrected_text": null,
  "editor_notes": "Minor terminology adjustment suggested."
}
```

Headers (from Knoxx auth):
- `X-Knoxx-User-Id`
- `X-Knoxx-User-Email`
- `X-Knoxx-Org-Id`
- `X-Knoxx-Tenant-Id`

This creates a `translation.label` event and updates segment status:
- `overall: approve` → status = `approved`
- `overall: needs_edit` + `corrected_text` → status = `approved`
- `overall: needs_edit` (no correction) → status = `in_review`
- `overall: reject` → status = `rejected`

### Batch Import Segments

```
POST /v1/translations/segments/batch
```

For MT pipeline to push translated segments.

Body:
```json
{
  "segments": [
    {
      "source_text": "...",
      "translated_text": "...",
      "source_lang": "en",
      "target_lang": "es",
      "document_id": "...",
      "segment_index": 0,
      "mt_model": "glm-5",
      "confidence": 0.87
    }
  ]
}
```

---

## Permissions

Add to `policy-db.mjs`:

```javascript
['org.translations.read', 'org_translations', 'read', 'Read translation segments'],
['org.translations.review', 'org_translations', 'review', 'Review and label translations'],
['org.translations.export', 'org_translations', 'export', 'Export translation training data'],
['org.translations.manage', 'org_translations', 'manage', 'Manage translation pipeline config'],
```

Add `translator` role to `ORG_ROLE_SEEDS`:

```javascript
{
  slug: 'translator',
  name: 'Translator',
  permissions: [
    'org.datalakes.read',
    'datalake.read',
    'agent.chat.use',
    'org.translations.read',
    'org.translations.review',
  ],
  toolPolicies: [
    { toolId: 'read', effect: 'allow' },
    { toolId: 'semantic_query', effect: 'allow' },
  ],
}
```

---

## Files to Create/Modify

| File | Purpose |
|------|---------|
| `orgs/open-hax/openplanner/src/routes/v1/translations.ts` | Translation segment CRUD routes |
| `orgs/open-hax/openplanner/src/lib/types.ts` | Add `TranslationSegmentEvent`, `TranslationLabelEvent` types |
| `orgs/open-hax/openplanner/src/routes/v1/index.ts` | Register translation routes |
| `orgs/open-hax/knoxx/backend/src/policy-db.mjs` | Add translation permissions + translator role |

---

## Exit Criteria

- [ ] `GET /v1/translations/segments` returns segments from MongoDB
- [ ] `GET /v1/translations/segments/:id` returns segment with labels
- [ ] `POST /v1/translations/segments/:id/labels` creates label event
- [ ] Labels update segment status correctly
- [ ] `POST /v1/translations/segments/batch` creates multiple segments
- [ ] `translator` role exists in policy-db with correct permissions
