# Simple Markdown Gardens

**Status:** Needs revision
**Created:** 2026-04-11
**Owner:** CMS/Gardens

Canonical update: 2026-04-12
See `knowledge-ops-translation-triage-2026-04-12.md`.
This spec currently conflicts with the live garden publication path. Translation is not removed from the garden model in practice. The canonical implementation actively uses `garden.target_languages`, garden-scoped publication metadata, and queued translation jobs during CMS publish.

## Overview

Gardens are CMS-managed publication targets that render markdown documents with a selectable uxx theme. Each garden is a simple static-ish site: a collection of markdown documents with consistent styling.

## Problem

Current "gardens" are hardcoded workbenches (query, ingestion, truth-workbench) that:
- Are independent web applications, not CMS-managed
- Have no connection to document content
- Cannot be styled or themed
- Cannot be created by users

We need gardens that:
- Are created and managed through CMS
- Render markdown documents from the document store
- Support selectable themes from the uxx design system
- Are simple static sites, not complex applications

## Simplified Data Model

### Garden Document (MongoDB `gardens` collection)

```typescript
interface Garden {
  _id: ObjectId;
  garden_id: string;           // URL-safe slug, unique
  title: string;               // Display name
  description?: string;        // Optional description
  
  // Theme configuration
  theme: string;               // uxx theme name (default, monokai, etc.)
  
  // Publication settings  
  default_language: string;    // Primary language (e.g., "en")
  target_languages?: string[]; // Optional translation targets (future)
  
  // Content filtering - which documents belong to this garden
  source_filter: {
    project?: string;          // e.g., "devel"
    kind?: string;             // e.g., "docs"
    domain?: string;           // Content domain filter
    path_prefix?: string;      // Path prefix filter
  };
  
  // Navigation
  nav?: {
    items: {
      label: string;
      path: string;
      children?: { label: string; path: string }[];
    }[];
  };
  
  // Metadata
  owner_id: string;
  created_by: string;
  created_at: Date;
  updated_at: Date;
  status: "draft" | "active" | "archived";
}
```

### Key Changes from Previous Model

1. Translation complexity is still present in the live system via garden-targeted publication metadata, `translation_jobs`, and public translation serving.
2. **Removed domain routing** - Gardens live at `/gardens/:garden_id/*`
3. **Added theme selection** - Simple string field for uxx theme
4. **Simplified source_filter** - Just project/kind/domain/path_prefix
5. **Added navigation config** - Custom nav structure for the garden

## API Surface

### Garden Management (existing, unchanged)

- `GET /v1/gardens` - List gardens
- `POST /v1/gardens` - Create garden
- `GET /v1/gardens/:id` - Get garden details
- `PATCH /v1/gardens/:id` - Update garden
- `DELETE /v1/gardens/:id` - Archive garden

### Garden Documents (existing, unchanged)

- `GET /v1/gardens/:id/documents` - List documents in garden
- `GET /v1/public/gardens/:garden_id` - Public garden landing
- `GET /v1/public/gardens/:garden_id/documents` - Public document list
- `GET /v1/public/gardens/:garden_id/documents/:doc_id` - Single document

## Rendering

### Theme Application

Each garden renders documents using its selected theme. The uxx package provides:

- `default` - Light theme with blue accents
- `monokai` - Dark theme (workspace default)
- `minimal` - Clean, minimal styling

### Markdown Rendering

Documents are stored as markdown content. Rendering flow:

```
GET /v1/public/gardens/:garden_id/documents/:doc_id
         │
         ▼
┌─────────────────────────────────────┐
│ 1. Load garden config (theme, etc)  │
│ 2. Load document content            │
│ 3. Apply source_filter to verify    │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 4. Render markdown to HTML          │
│ 5. Apply garden theme CSS           │
│ 6. Add garden navigation            │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 7. Return HTML or JSON (content-neg)│
└─────────────────────────────────────┘
```

## Frontend Components

### Garden List Page (`/gardens`)

Shows all gardens the user can access with:
- Garden title and description
- Theme preview
- Document count
- Links to view/manage

### Garden View Page (`/gardens/:garden_id`)

Renders the garden as a public site:
- Applies selected theme
- Shows navigation
- Lists documents
- Renders individual documents

### Garden Admin Page (`/admin/gardens`)

For managing gardens:
- Create/edit/delete gardens
- Configure theme
- Set source filters
- Customize navigation

## Implementation Plan

### Phase 1: Simplify Model
- Remove translation-related fields from garden schema
- Add `theme` field with default value
- Simplify `source_filter` structure
- Add `nav` configuration

### Phase 2: Theme Integration
- Import uxx themes into garden rendering
- Create theme selection UI in admin
- Apply theme CSS to rendered documents

### Phase 3: Navigation
- Implement nav configuration
- Auto-generate nav from document structure
- Support custom nav overrides

### Phase 4: Public Views
- Build garden landing page component
- Document list with theme
- Single document view with theme
- Syntax highlighting for code blocks

## Example Garden

```json
{
  "garden_id": "knoxx-docs",
  "title": "Knoxx Documentation",
  "description": "User guide and API reference for Knoxx",
  "theme": "monokai",
  "default_language": "en",
  "source_filter": {
    "project": "knoxx",
    "kind": "docs"
  },
  "nav": {
    "items": [
      { "label": "Getting Started", "path": "/getting-started" },
      { "label": "API Reference", "path": "/api" },
      { 
        "label": "Guides", 
        "path": "/guides",
        "children": [
          { "label": "Chat", "path": "/guides/chat" },
          { "label": "CMS", "path": "/guides/cms" }
        ]
      }
    ]
  },
  "status": "active"
}
```

## Success Criteria

- Gardens can be created/managed via CMS UI
- Each garden has a selectable theme
- Documents are rendered with applied theme
- Navigation reflects garden structure
- Public views work without authentication
