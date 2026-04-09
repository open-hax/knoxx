# Knowledge Ops — Unified Knowledge Workbench UI Spec

## Current Canonical Reading

- state: current UI direction, partially updated to Knoxx vocabulary
- canonical Knoxx workbench terms today:
  - left rail = **Context Bar**
  - center = **Agent Runtime**
  - right rail = **Scratchpad**
- interpret older `Canvas` language in this doc as the predecessor of the current Knoxx **Scratchpad** surface

> *One interface. File explorer ingestion, collection chat, labeling, and synthesis canvas. Every panel shares the same data sources.*

---

## Purpose

Define the unified UI for the knowledge workbench: a single-page application where knowledge workers explore files, ingest them into collections, chat with collections, label data, and create deliverables on a synthesis canvas.

All primitives and composites in this UI come from:
- `orgs/open-hax/uxx/tokens` (`@open-hax/uxx/tokens`)
- `orgs/open-hax/uxx/react` (`@open-hax/uxx`)

No page in the workbench should implement its own Button, Card, Input, Modal, mode indicator, or keyboard overlay.

---

## The Problem

Current UI is fragmented:
- **Ingestion page** (`/ingestion`) — create sources, run jobs. No file browsing. No labeling. No collection awareness.
- **Chat Lab** (`/`) — talk to models. No collection context. No labeling. No deliverable export.
- **Query page** (`/query`) — federated search. No chat. No labeling. No collection browsing.
- **CMS** (`/cms`) — document CRUD. No chat. No ingestion. No collection view.
- **Gardens** (`devel-deps-garden`, `eta-mu-truth-workbench`) — useful operator surfaces, but detached from the rest of the workbench.

Each page talks to different backends. No shared state. No shared data sources.

---

## Vision: One Workbench

A single-page app with five primary panels plus optional garden views, all sharing the same data sources:

```
┌─────────────────────────────────────────────────────────────┐
│ Knowledge Workbench                                    [user]│
├─────────┬───────────────────────────────────────────────────┤
│         │                                                   │
│  Nav    │  Scratchpad / work surface                        │
│         │  (the main content area — switches by mode)       │
│ Files   │                                                   │
│ Chat    │                                                   │
│ Labels  │                                                   │
│ Synth   │                                                   │
│ Gardens │                                                   │
│         │                                                   │
├─────────┴───────────────────────────────────────────────────┤
│  Status bar: collection, provider, model, tokens             │
└─────────────────────────────────────────────────────────────┘
```

---

## Panel 1: File Explorer (Ingestion)

### Purpose

Browse workspace files, select folders/files, route them into collections.

### UI

```
┌─────────────────────────────────────────────────────────────┐
│ Browse Files                                                 │
├──────────────────────┬──────────────────────────────────────┤
│ Workspace Tree       │ File Preview                         │
│                      │                                      │
│ 📁 docs/             │ 📄 knowledge-ops-the-lake.md        │
│   📄 README.md       │                                      │
│   📄 notes/          │ The Lake is the spine of the system │
│     📄 todo.md       │ OpenPlanner is the data layer...    │
│ 📁 specs/            │                                      │
│   📁 drafts/         │ [Route to: docs ▾]  [Skip]          │
│     📄 spec1.md      │                                      │
│     📄 spec2.md      │ Tags: [architecture] [x] [lake] [ ] │
│ 📁 src/              │                                      │
│   📁 lib/            │                                      │
│   📁 api/            │                                      │
│ 📁 services/         │                                      │
│                      │                                      │
├──────────────────────┴──────────────────────────────────────┤
│ Selected: 14 files (2.1 MB)                        [Ingest]│
└─────────────────────────────────────────────────────────────┘
```

### Features

- **Workspace tree** — browse the mounted workspace directory. Filter by file type (`.md`, `.clj`, `.ts`, etc.)
- **File preview** — read a file before ingesting
- **Route to collection** — select which collection each file goes to (e.g., `devel-docs`, `devel-code`, `devel-config`)
- **Tag files** — add labels/tags to files before ingest
- **Bulk select** — select multiple files, route them together
- **Ingest button** — trigger ingestion of selected files into selected collection

### UI library requirements

- Explorer shell uses `Card`
- all actions use `Button`
- file filters use `Input`
- mode is shown with `ModeIndicator`
- keyboard discoverability comes from `ChordOverlay`

### Backend

- `GET /api/cms/files?path=/app/workspace&ext=.md,.clj` — list files
- `GET /api/cms/files/content?path=...` — read file content
- `POST /api/ingestion/ingest` — ingest selected files into a collection

---

## Panel 2: Chat (Collection-Aware)

### Purpose

Talk to specific collections. Each chat is scoped to a collection.

### UI

```
┌─────────────────────────────────────────────────────────────┐
│ Chat with: [devel-docs ▾]                                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ What is the Lake?                                            │
│                                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ The Lake is the data layer of the Promethean platform.  │ │
│ │ It uses OpenPlanner for event ingestion, ChromaDB/Mongo │ │
│ │ for vector search, and semantic compaction for building  │ │
│ │ knowledge packs...                                      │ │
│ │                                                         │ │
│ │ Sources: [knowledge-ops-the-lake.md] [specs/...]       │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ How does compaction work?                                    │
│                                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ [streaming...]                                           │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│ [Ask a question...]                                    [→] │
└─────────────────────────────────────────────────────────────┘
```

### Features

- **Collection selector** — pick which collection to chat with
- **Scoped chat** — each conversation is bound to a collection
- **Source citations** — every answer shows which files it came from
- **Cross-collection** — optional: search across multiple collections
- **Export to canvas** — send an answer to the synthesis canvas for editing

### UI library requirements

- chat composer is built on `@open-hax/uxx` primitives and `@workspace/chat-ui`
- collection selector uses `Input`/select contract styling from `orgs/open-hax/uxx/`
- source citations use shared badge/card styling

### Backend

- `GET /api/query/search` — scoped to collection
- `POST /api/query/answer` — scoped to collection, returns grounded answer

---

## Panel 3: Labels (Expert Review)

### Purpose

Label data for training. Show items that need review.

### UI

```
┌─────────────────────────────────────────────────────────────┐
│ Review Queue                                    [4 pending] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Question                                                    │
│  "How do I reset my SSO token?"                             │
│                                                              │
│  Context                                                     │
│  [SSO Runbook §3.2] [Auth Guide §2.1]                       │
│                                                              │
│  Model Answer                                                │
│  "Navigate to Settings → Security → Reset Token"            │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ correctness:    [partially-correct ▾]                   ││
│  │ groundedness:   [hallucinated ▾]                        ││
│  │ risk:           [safe ▾]                                ││
│  │ overall:        [needs-edit ▾]                          ││
│  │ gold answer:    [Contact your admin at admin@acme.com]  ││
│  │ editor notes:   [SSO tokens are managed by IT, not self]││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  [Save & Next]  [Skip]  [Export to Scratchpad]              │
└─────────────────────────────────────────────────────────────┘
```

### Features

- **Review queue** — show items needing expert review
- **Label form** — 8-dimension labeling (correctness, groundedness, completeness, tone, risk, PII leakage, translation quality, overall)
- **Gold answer** — expert provides corrected answer
- **Export to canvas** — send labeled item to synthesis canvas for inclusion in deliverables
- **Batch operations** — approve multiple items at once

### UI library requirements

- label controls use `Input`, `Button`, `Card`, `Badge`
- review mode should expose chord hints for approve/reject/edit operations

### Backend

- `GET /api/km-labels/{tenant_id}?overall=pending` — get review queue
- `PATCH /api/km-labels/{tenant_id}/{id}` — update labels
- `GET /api/export/{tenant_id}/sft` — export training data

---

## Panel 4: Synthesize (Scratchpad)

### Purpose

Create deliverables from search results, chat answers, and labeled data. Like ChatGPT's canvas feature: produces clean output without conversational boilerplate.

### UI

```
┌─────────────────────────────────────────────────────────────┐
│ Synthesize Scratchpad                                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Sources                                                     │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ [knowledge-ops-the-lake.md]          → from Chat        ││
│  │ [SSO Runbook §3.2]                   → from Labels     ││
│  │ [devel-docs: deployment checklist]   → from Search     ││
│  │ [+ Add from collection...]                              ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  Synthesis Prompt                                            │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ Write a deployment checklist for the Lake platform      ││
│  │ using the provided context. Format as markdown.          ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  Output                                                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ # Lake Platform Deployment Checklist                     ││
│  │                                                         ││
│  │ ## Prerequisites                                        ││
│  │ - Docker and Docker Compose installed                   ││
│  │ - PostgreSQL 16+ running                                ││
│  │ - MongoDB 8.2+ with mongot sidecar                      ││
│  │ ...                                                     ││
│  │                                                         ││
│  │ [Copy]  [Export PDF]  [Export Markdown]  [Save to CMS] ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  [Generate]  [Regenerate]  [Edit Output]                     │
└─────────────────────────────────────────────────────────────┘
```

### Features

- **Source assembly** — drag in files, chat answers, labeled items, search results
- **Synthesis prompt** — describe what to create from the sources
- **Clean output** — no conversational boilerplate, just the deliverable
- **Edit in-place** — tweak the output before exporting
- **Export** — copy, PDF, markdown, save to CMS as a document
- **Save to CMS** — push the deliverable into the CMS as a new document for future reference

### UI library requirements

- canvas shell uses `Card`
- all export actions use shared `Button`
- synthesis status should be shown in the status bar with `ModeIndicator`

---

## Panel 5: Gardens

### Purpose

Expose non-chat operator surfaces inside the same workbench.

### Initial gardens

| Garden | Purpose |
|------|---------|
| Dependency Garden | review workspace dependency topology |
| Truth Garden | review truth/control-plane state |

### UI

Gardens are opened from the same left nav and share:
- status bar
- keyboard system
- component library
- lake/project selection context where relevant

### Backend

- `POST /api/query/answer` — with specific sources, returns synthesis
- `POST /api/cms/documents` — save deliverable to CMS
- `POST /api/export/pdf` — export to PDF

---

## Shared Data Layer

All four panels share the same data sources:

```
┌─────────────────────────────────────────────────────────────┐
│                  Shared Data Sources                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  collections:                                                │
│    devel-docs     ← docs/, specs/, notes/                   │
│    devel-code     ← src/, packages/, orgs/                  │
│    devel-config   ← services/, compose files, env examples  │
│    devel-data     ← data/, datasets/, reports/              │
│    devel-events   ← OpenPlanner live event stream           │
│    public_docs    ← CMS-published documents only             │
│                                                              │
│  search:                                                     │
│    OpenPlanner FTS + vector search (federated)              │
│                                                              │
│  labels:                                                     │
│    km_labels DB (PostgreSQL)                                │
│                                                              │
│  LLM:                                                        │
│    Proxx (glm-5, glm-4.7, etc.)                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Existing Code References

| File | What it has | What's missing |
|------|-------------|----------------|
| `orgs/mojomast/ragussy/frontend/src/pages/IngestionPage.tsx` | Source CRUD, job polling | File browsing, collection routing |
| `orgs/mojomast/ragussy/frontend/src/pages/ChatLabPage.tsx` | Chat with Proxx/Ragussy | Collection scoping, source citations |
| `orgs/mojomast/ragussy/frontend/src/pages/QueryPage.tsx` | Federated search, presets | Collection selector, chat integration |
| `orgs/mojomast/ragussy/frontend/src/pages/CmsPage.tsx` | Document CRUD, visibility | Chat integration, canvas export |
| `orgs/mojomast/ragussy/frontend/src/components/KnowledgeLabeler.tsx` | 8-dimension label form | Review queue, export to canvas |
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj` | Ingestion API, query presets | File browsing endpoint |
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/drivers/local.clj` | File discovery, filtering | File content reading endpoint |
| `orgs/open-hax/uxx/react/src/primitives/*` | Shared Monokai primitives | Need adoption across pages |
| `orgs/open-hax/uxx/react/src/composites/ChordOverlay.tsx` | Spacemacs-style chord discoverability | Need integration into workbench shell |
| `orgs/open-hax/uxx/react/src/primitives/ModeIndicator.tsx` | Modal mode indicator | Need integration into status bar |
| `services/devel-deps-garden/README.md` | Dependency Garden surface | Needs integration into workbench shell |
| `services/eta-mu-truth-workbench/README.md` | Truth Garden surface | Needs integration into workbench shell |

---

## Implementation Order

1. **Fix Chat Lab** — done (ragussy health URL fixed)
2. **Collection-scoped chat** — modify Chat Lab to accept a collection context
3. **File explorer** — add file browsing endpoint + tree component
4. **Ingestion from explorer** — route selected files to collections
5. **Labeling from chat** — export chat answers to review queue
6. **Scratchpad synthesis** — assembly panel + clean output

---

## Status

Specified 2026-04-02. Based on user feedback about Chat Lab, ingestion UI, and synthesis canvas.
