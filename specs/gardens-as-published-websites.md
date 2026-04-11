# Gardens as Published Websites

**Status:** Draft
**Created:** 2025-04-11
**Owner:** CMS/Gardens/Ingestion Pivot

## Overview

Gardens are user-managed publication targets that expose curated document collections as public websites. Each garden has its own domain, target languages for translation, and publication rules. This spec defines the data model, API surface, and integration points for gardens as a first-class concept in the Knoxx stack.

## Problem Statement

Currently:
- Gardens are hardcoded stubs (query, ingestion, dependency, truth-workbench) representing operator workbenches
- Documents have `visibility` (internal/review/public/archived) but no publication target
- Translation exists but isn't wired to publication flow
- No way for users to create/manage gardens as publication destinations

Users need:
- Create gardens with custom slugs and target languages
- Publish documents to specific gardens
- Automatic translation pipeline when publishing
- Public garden views (the "published website" aspect)

## Data Model

### Garden Document (MongoDB `gardens` collection)

```typescript
interface Garden {
  _id: ObjectId;
  garden_id: string;           // URL-safe slug, unique
  title: string;               // Display name
  description?: string;        // Optional description
  
  // Publication settings
  domain: string;              // e.g., "docs.example.com" or "internal"
  base_path?: string;          // e.g., "/api/gardens/{garden_id}"
  default_language: string;    // Primary language (e.g., "en")
  target_languages: string[];  // Languages to translate to (e.g., ["es", "de", "fr"])
  
  // Content filtering
  source_filter?: {
    projects?: string[];       // Limit to specific projects
    domains?: string[];        // Content domains (e.g., ["api", "tutorial"])
    tags?: string[];           // Content tags
  };
  
  // Publication rules
  auto_translate: boolean;     // Auto-queue translation on publish
  require_review: boolean;     // Require SME review before public
  visibility_default: "internal" | "review" | "public";
  
  // Metadata
  owner_id: string;            // User/org that owns the garden
  created_by: string;
  created_at: Date;
  updated_at: Date;
  
  // Status
  status: "draft" | "active" | "archived";
  
  // Analytics (optional, populated by background jobs)
  stats?: {
    documents_count: number;
    translations_count: number;
    last_published_at?: Date;
  };
}
```

### Garden Publication (embedded in document metadata)

Documents published to gardens have their metadata updated:

```typescript
interface DocumentMetadata {
  // ... existing fields ...
  garden_publications?: {
    garden_id: string;
    published_at: string;
    published_by: string;
    translation_status: "pending" | "in_progress" | "completed" | "failed";
    translated_languages?: string[];
  }[];
}
```

### Translation Job Extension

Translation jobs reference the garden publication:

```typescript
interface TranslationJob {
  // ... existing fields ...
  garden_id?: string;          // If translation triggered by garden publish
  publication_id?: string;     // Links to specific publication event
}
```

## API Surface

### Garden Management

#### `GET /v1/gardens`
List all gardens the user can access.

**Response:**
```json
{
  "gardens": [
    {
      "garden_id": "developer-docs",
      "title": "Developer Documentation",
      "domain": "docs.example.com",
      "default_language": "en",
      "target_languages": ["es", "de"],
      "status": "active",
      "stats": {
        "documents_count": 42,
        "translations_count": 84
      }
    }
  ],
  "total": 1
}
```

#### `POST /v1/gardens`
Create a new garden.

**Request:**
```json
{
  "garden_id": "developer-docs",
  "title": "Developer Documentation",
  "description": "Public API documentation",
  "domain": "docs.example.com",
  "default_language": "en",
  "target_languages": ["es", "de", "fr"],
  "auto_translate": true,
  "require_review": true,
  "source_filter": {
    "domains": ["api", "tutorial"]
  }
}
```

**Response:** Created garden object.

#### `GET /v1/gardens/:id`
Get garden details.

#### `PATCH /v1/gardens/:id`
Update garden settings.

#### `DELETE /v1/gardens/:id`
Archive garden (soft delete, sets status to "archived").

### Garden Publication

#### `POST /v1/cms/documents/:id/publish/:garden_id`
Publish document to a garden.

**Request (optional body):**
```json
{
  "skip_translation": false,
  "target_languages_override": ["es"],
  "notes": "Initial publication"
}
```

**Response:**
```json
{
  "status": "published",
  "doc_id": "uuid",
  "garden_id": "developer-docs",
  "visibility": "public",
  "translation_jobs": [
    {
      "job_id": "uuid",
      "target_lang": "es",
      "status": "queued"
    },
    {
      "job_id": "uuid",
      "target_lang": "de",
      "status": "queued"
    }
  ]
}
```

#### `DELETE /v1/cms/documents/:id/publish/:garden_id`
Unpublish document from garden (sets visibility to "archived" for that garden).

#### `GET /v1/gardens/:id/documents`
List documents published to a garden.

**Query params:**
- `language`: Filter by language (default: garden's default_language)
- `visibility`: Filter by visibility
- `domain`: Filter by content domain
- `limit`, `offset`: Pagination

### Public Garden Views

#### `GET /v1/public/gardens/:garden_id`
Public endpoint for garden landing page.

**Response:**
```json
{
  "garden": {
    "title": "Developer Documentation",
    "description": "Public API documentation"
  },
  "languages": ["en", "es", "de", "fr"],
  "stats": {
    "documents_count": 42
  }
}
```

#### `GET /v1/public/gardens/:garden_id/documents`
Public documents in a garden (only `visibility: "public"`).

**Query params:**
- `language`: Requested language (returns translated version if available)
- `path`: Filter by path prefix
- `search`: Full-text search

#### `GET /v1/public/gardens/:garden_id/documents/:doc_id`
Single document in a garden.

**Query params:**
- `language`: Requested language

**Response includes:**
- Original document OR translated version if available
- Language negotiation metadata
- Links to other language versions

## Translation Integration

### Publication Flow

```
POST /cms/documents/:id/publish/:garden_id
         │
         ▼
┌─────────────────────────────────────┐
│ 1. Validate document exists         │
│ 2. Validate garden exists           │
│ 3. Check permissions                │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 4. Update document visibility       │
│    to "public"                      │
│ 5. Add garden_publications entry    │
│    to document metadata             │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 6. If garden.auto_translate:        │
│    - For each target_language:      │
│      - Create translation job       │
│      - Queue for MT pipeline        │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 7. Return publication status        │
│    with translation job IDs         │
└─────────────────────────────────────┘
```

### Translation Completion Flow

```
MT Pipeline completes translation
         │
         ▼
┌─────────────────────────────────────┐
│ 1. Update translation segment status│
│    to "approved" or "in_review"     │
│ 2. If require_review: notify SME    │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 3. Update document's                │
│    garden_publications entry:       │
│    - Append to translated_languages │
│    - Update translation_status      │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 4. Document now available in        │
│    public garden in that language   │
└─────────────────────────────────────┘
```

### SME Review Interface

#### `GET /v1/translations/review`
Get segments pending review for a garden.

**Query params:**
- `garden_id`: Filter by garden
- `target_lang`: Filter by target language
- `limit`, `offset`: Pagination

#### `POST /v1/translations/segments/:id/review`
Submit SME review for a segment.

**Request:**
```json
{
  "adequacy": "good",
  "fluency": "excellent",
  "terminology": "correct",
  "risk": "safe",
  "overall": "approve",
  "corrected_text": "Optional corrected translation",
  "editor_notes": "Optional notes"
}
```

## Implementation Phases

### Phase 1: Garden Data Model & Management API
- Create `gardens` MongoDB collection
- Add garden CRUD routes (`/v1/gardens`)
- Migrate hardcoded gardens to database
- Add garden stats aggregation

### Phase 2: Publication Flow
- Extend document metadata for `garden_publications`
- Implement `POST /cms/documents/:id/publish/:garden_id`
- Add garden document listing endpoints
- Update CMS frontend to show garden dropdown

### Phase 3: Translation Integration
- Wire publication to translation job creation
- Add `garden_id` to translation jobs
- Implement translation status tracking per garden
- Add SME review notification system

### Phase 4: Public Garden Views
- Implement `/v1/public/gardens/*` endpoints
- Add language negotiation
- Build garden landing pages
- Add full-text search within garden

### Phase 5: Frontend
- CMS page: garden selection dropdown
- Garden management page
- Translation review interface
- Public garden browsing

## Migration Path

### From Hardcoded Gardens

```javascript
// Seed gardens collection with existing hardcoded definitions
const seedGardens = [
  {
    garden_id: "query",
    title: "Query Garden",
    domain: "internal",
    default_language: "en",
    target_languages: [],
    auto_translate: false,
    status: "active",
    // ... map from hardcoded structure
  },
  // ... other hardcoded gardens
];
```

### From visibility-only Documents

Documents currently have `visibility` but no `garden_id`:

```javascript
// Migration script to add garden_publications
db.events.updateMany(
  { kind: "docs", "extra.visibility": "public" },
  {
    $set: {
      "extra.garden_publications": [
        {
          garden_id: "default",
          published_at: "$ts",
          published_by: "migration",
          translation_status: "completed"
        }
      ]
    }
  }
);
```

## Permissions

| Action | Role Required |
|--------|---------------|
| List gardens | Authenticated user |
| Create garden | Garden admin |
| Update garden | Garden owner/admin |
| Delete garden | Garden owner |
| Publish to garden | Garden contributor |
| Review translations | Garden SME |
| View public garden | Anonymous |

## Open Questions

1. **Domain routing**: Should gardens support custom domains via DNS/CNAME, or only subpaths under the main domain?
   
   *Recommendation*: Start with subpaths (`/gardens/:garden_id/*`), add custom domains later.

2. **Versioning**: Should republishing create new versions or update in place?
   
   *Recommendation*: Update in place, keep history via event log (already in OpenPlanner).

3. **Translation quality gates**: Should documents be hidden until translation reaches "approved" status?
   
   *Recommendation*: Configurable per garden via `require_review` flag.

4. **Batch operations**: How to handle publishing many documents at once?
   
   *Recommendation*: Add `POST /v1/gardens/:id/batch-publish` endpoint that queues jobs.

## Success Metrics

- Gardens can be created/managed via API
- Documents can be published to specific gardens
- Translations auto-queue when publishing to gardens with `auto_translate: true`
- Public garden views show documents in requested language
- SME review interface shows garden context

## References

- `knowledge-ops-gardens.md` - Original garden concept (workbenches)
- `knowledge-ops-cms-data-model.md` - CMS document model
- `knowledge-ops-ingestion-pipeline.md` - Ingestion architecture
- OpenPlanner MongoDB `events` collection - Document store
- Translation routes in `src/routes/v1/translations.ts`
