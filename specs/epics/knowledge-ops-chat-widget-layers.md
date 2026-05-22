# Knowledge Ops — Platform Layer Architecture Spec

> *Five layers. The CMS is the boundary between public and internal. The widget is the tip of the spear.*

---

## Purpose

Define the full LLM layer stack for the futuresight-kms platform, with the chat widget scoped to Layer 1 (public assistant only), and an agent-aware CMS as the critical curation boundary between public and internal knowledge.

All UI surfaces described here use:
- `orgs/open-hax/uxx/tokens` (`@open-hax/uxx/tokens`)
- `orgs/open-hax/uxx/react` (`@open-hax/uxx`)

The widget, CMS, and workbench should share one visual language and one keyboard system.

---

## The Five Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 1: PUBLIC ASSISTANT                                      │
│  The chat widget. Customer-facing. "Ask us anything."           │
│  Only sees curated public knowledge.                            │
│  ← this is what the widget does                                 │
├─────────────────────────────────────────────────────────────────┤
│  LAYER 2: AGENT-AWARE CMS  ← NEW                               │
│  Knowledge workers curate what's public vs internal.            │
│  AI drafts content, workers approve/publish.                    │
│  This is the boundary layer.                                    │
├─────────────────────────────────────────────────────────────────┤
│  LAYER 3: KNOWLEDGE WORKER                                      │
│  Employee-facing. Internal knowledge base.                      │
│  Draft proposals, SOPs, runbooks. Full internal corpus.         │
├─────────────────────────────────────────────────────────────────┤
│  LAYER 4: CODING AGENT  (optional, dev teams only)              │
│  Code generation, refactoring, PRs.                             │
│  Stronger model, more tools, sandboxed execution.               │
├─────────────────────────────────────────────────────────────────┤
│  LAYER 5: SME REVIEW                                            │
│  Powered by Shibboleth. Translation QA, data labeling.          │
│  Expert-facing. Feeds training pipeline.                        │
│  ← not a widget, it's the Shibboleth chat lab                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Layer 1: Public Assistant (The Widget)

**Scope**: This is what the chat widget does. Nothing more.

- Floating button, bottom-right. Same pattern as Intercom/Crisp.
- Answers from **curated public knowledge only** — Layer 2 publishes to this layer.
- Can set appointments, answer FAQ, hand off to human when confidence is low.
- Tenant-branded. Each client gets their own widget instance.

**What it sees**: Only documents marked `visibility: public` by Layer 2.

**What it cannot see**: Internal docs, drafts, specs, proposals, code, anything behind the CMS boundary.

```
┌─────────────────────────────────────┐
│ Ask Us Anything            [─][×]   │
├─────────────────────────────────────┤
│                                     │
│  ┌───────────────────────────────┐  │
│  │ Welcome! How can we help?     │  │
│  │                               │  │
│  │ [What services do you offer?] │  │
│  │ [Book a consultation]         │  │
│  │ [How does pricing work?]      │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Type your question...    [→]  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Backend**: canonical target is OpenPlanner query + Proxx synthesis with visibility filtered to `public`.

**Files**:
| File | Purpose |
|------|---------|
| `packages/futuresight-kms/frontend/components/ChatWidget.tsx` | Floating button + expanded chat panel |
| `packages/futuresight-kms/frontend/components/ChatWidget.css` | Widget styles |

That's it for the widget. It's a simple RAG chat against the public collection.

---

## Layer 2: Agent-Aware CMS (The Boundary)

**This is the new layer.** This is where the magic happens.

### What it is

An AI-assisted content management system where knowledge workers:
- **Curate** what becomes public knowledge (Layer 1 reads from here)
- **Draft** content with AI assistance (blog posts, FAQ entries, service descriptions)
- **Review** AI-generated drafts before publishing
- **Translate** content for multilingual clients
- **Control** the public/internal boundary per document

### The boundary problem

Without this layer, there's no controlled handoff between internal knowledge and the public chat widget. Either:
- The widget sees everything (leak risk), or
- The widget sees nothing useful (dead product)

The CMS solves this by being the **publishing gate**: internal knowledge flows in, curated public knowledge flows out to Layer 1.

Canonical architecture note:
- the CMS is not a second store
- it is a metadata and workflow layer over OpenPlanner

### Document visibility model

```
┌──────────────────────────────────────────────────┐
│                INTERNAL CORPUS                    │
│  specs, runbooks, proposals, code, drafts,       │
│  meeting notes, financial data, PII-bearing docs │
│                                                  │
│  visibility: internal (default)                  │
│  Layer 1 cannot see these.                       │
├──────────────────────────────────────────────────┤
│                CMS BOUNDARY                      │
│  Knowledge worker reviews, AI drafts,            │
│  content is edited and approved for public use.  │
│                                                  │
│  visibility: review (pending public)             │
├──────────────────────────────────────────────────┤
│                PUBLIC CORPUS                      │
│  Service descriptions, FAQ, pricing,             │
│  blog posts, documentation, onboarding guides    │
│                                                  │
│  visibility: public (published)                  │
│  Layer 1 reads from this.                        │
└──────────────────────────────────────────────────┘
```

### Visibility states

| State | Meaning | Who can see | Layer 1 visible? |
|-------|---------|-------------|:---:|
| `internal` | Default. Raw knowledge. | Employees only | No |
| `review` | AI-drafted or worker-submitted, pending publish | CMS editors | No |
| `public` | Approved and published | Everyone | Yes |
| `archived` | Was public, pulled back | CMS editors | No |

### CMS UI concept

```
┌─────────────────────────────────────────────────────────────┐
│ Knowledge CMS — Acme Corp                                   │
├──────────┬──────────────────────────────────────────────────┤
│ Nav      │ Content Library                                  │
│          │                                                  │
│ Drafts   │ ┌─ Filter: [All ▾] [Internal] [Review] [Public] │
│ Review   │ │                                                  │
│ Public   │ │ ☐ Service Overview        ● public   2d ago    │
│ Internal │ │ ☐ Pricing FAQ             ● public   1w ago    │
│ Archive  │ │ ☐ Deployment Runbook      ● internal 3h ago    │
│          │ │ ☐ New Feature Blog Draft  ● review   1h ago    │
│          │ │ ☐ Integration Guide       ● internal 2d ago    │
│          │ │                                                  │
│          │ │ [Draft with AI]  [Move to Review]  [Publish]    │
│          │ └──────────────────────────────────────────────────│
│          │                                                  │
│          │ ┌─ AI Draft Assistant ──────────────────────────┐ │
│          │ │ Topic: "Our onboarding process"                │ │
│          │ │ Tone: [Professional ▾]  Audience: [Prospects]  │ │
│          │ │ Sources: [internal docs] [existing FAQ]         │ │
│          │ │                                                 │ │
│          │ │ [Generate Draft]                                │ │
│          │ │                                                 │ │
│          │ │ ┌─────────────────────────────────────────┐    │ │
│          │ │ │ AI-generated draft appears here...       │    │ │
│          │ │ │ Worker edits, approves, publishes.       │    │ │
│          │ │ └─────────────────────────────────────────┘    │ │
│          │ │                                                 │ │
│          │ │ [Edit] [Send to Review] [Publish Directly]     │ │
│          │ └─────────────────────────────────────────────────┘ │
└──────────┴──────────────────────────────────────────────────┘
```

### AI-assisted content workflows

| Workflow | Trigger | What the AI does | Human does |
|----------|---------|-----------------|------------|
| FAQ from internal docs | Worker selects internal doc | Summarizes into FAQ format, strips PII, proposes public version | Reviews, edits tone, approves |
| Blog draft | Worker provides topic | Generates draft from internal knowledge + web research | Edits, adds voice, publishes |
| Service description | New service added internally | Drafts customer-facing description from technical spec | Reviews accuracy, approves |
| Translation | Public doc needs other language | Translates, flags uncertain segments | Reviews translation quality |
| Content refresh | Public doc goes stale | Detects staleness via internal changes, drafts update | Approves or rejects |

### Data model additions

```ts
Document {
  doc_id: string
  tenant_id: string
  title: string
  content: string
  visibility: "internal" | "review" | "public" | "archived"
  source: "manual" | "ai-drafted" | "ingested"
  source_path: string           // original file path if ingested
  domain: string
  language: string
  created_by: string
  published_by: string | null
  published_at: string | null
  last_reviewed_at: string | null
  ai_drafted: boolean
  ai_model: string | null       // which model generated the draft
  ai_prompt_hash: string | null // reproducibility
  metadata: Record<string, unknown>
}
```

### API surface

| Endpoint | Purpose |
|----------|---------|
| `GET /api/cms/documents` | List docs, filter by visibility, domain, source |
| `POST /api/cms/documents` | Create manual doc |
| `PATCH /api/cms/documents/{id}` | Update content, visibility, metadata |
| `POST /api/cms/draft` | AI-generate a draft from topic + sources + tone |
| `POST /api/cms/publish/{id}` | Set visibility to `public` (feeds Layer 1) |
| `POST /api/cms/archive/{id}` | Pull doc from public back to `archived` |
| `GET /api/cms/public` | Returns all `public` docs (Layer 1 reads this) |

### Backend

The CMS backend is a new router in the km_labels Python package. The public docs collection in Qdrant is synced from `visibility = public` documents.

**Files to create**:

| File | Purpose |
|------|---------|
| `packages/futuresight-kms/python/km_labels/models.py` | Add `Document` model with visibility states |
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | CMS CRUD, draft generation, publish/archive |
| `packages/futuresight-kms/python/km_labels/database.py` | Add `documents` table with visibility, source tracking |
| `packages/futuresight-kms/frontend/components/CmsDashboard.tsx` | CMS UI: content library, filters, draft assistant |
| `packages/futuresight-kms/frontend/components/CmsDraftAssistant.tsx` | AI draft panel: topic, tone, audience, sources |
| `packages/futuresight-kms/frontend/components/CmsContentRow.tsx` | Single doc row with visibility badge, actions |
| `packages/futuresight-kms/scripts/sync_public_collection.py` | Sync `visibility=public` docs to Qdrant `public_docs` collection |

### Qdrant collection architecture

| Collection | Contents | Fed by | Read by |
|-----------|----------|--------|---------|
| `devel_docs` | All ingested docs (raw) | `ingest_docs.py` | Layer 3 (knowledge workers) |
| `devel_specs` | Specs and design docs | `ingest_docs.py --collection devel_specs` | Layer 3 |
| `public_docs` | Curated public docs only | CMS publish action → `sync_public_collection.py` | Layer 1 (widget) |

---

## Layer 3: Knowledge Worker

**Scope**: Employee-facing internal knowledge base. Full corpus access.

- Draft proposals, SOPs, runbooks, content
- Search across all internal docs
- No public/private distinction — everything is internal here
- This is where `devel_docs` and `devel_specs` collections live

**Backend**: `POST /api/rag/search` + `/v1/chat/completions`

**Files**: Uses existing Ragussy frontend (Next UI at `/next/dashboard`, `/next/chat`, `/next/documents`).

---

## Layer 4: Coding Agent (Optional)

**Scope**: Dev teams only. Code generation, refactoring, PRs.

- Stronger model (qwen-14b or OpenAI)
- File read, shell exec (sandboxed via Openclawssy)
- Workspace-aware: reads from `packages/`, `services/`, `orgs/`

**Backend**: `POST /v1/chat/completions` with tool calls

**Not part of the CMS or the widget.** This is a separate tool surface.

---

## Layer 5: SME Review (Shibboleth)

**Scope**: Expert review and labeling. Powered by Shibboleth.

- Translation QA
- Data labeling with harm/response classes
- Training export pipeline
- Uses Shibboleth chat lab UI (port 8097 in the futuresight-kms stack)

**Not a widget.** Not part of the CMS. This is the Shibboleth control plane + UI.

**Integration point**: `POST /api/shibboleth/handoff` from Ragussy pushes conversations here for review.

---

## Revised Layer Configuration

| Layer | Who | What | Backend | Model | Widget? |
|-------|-----|------|---------|-------|:---:|
| 1. Public Assistant | Customers | FAQ, appointments, sales | Ragussy `/api/ragussy/chat` | `glm-4.7-flash` | **Yes** |
| 2. Agent-Aware CMS | Knowledge workers | Curate, draft, publish | km_labels `/api/cms/*` + Ragussy for AI drafts | `glm-4.7-flash` | No (full page) |
| 3. Knowledge Worker | Employees | Internal search, drafting | Ragussy RAG + LLM | `glm-4.7-flash` / `qwen-14b` | No (full page) |
| 4. Coding Agent | Devs | Code gen, refactoring | Ragussy OpenAI-compat + tools | `qwen-14b`+ | No (CLI/IDE) |
| 5. SME Review | Experts | Label, translate, correct | Shibboleth control plane | Human-driven | No (Shibboleth UI) |

---

## Demo Flow: devel as Tenant

| Step | Layer | Action | Shows |
|------|-------|--------|-------|
| 1 | CMS | Knowledge worker sees all internal docs, selects "Service Overview" | CMS content library with visibility badges |
| 2 | CMS | Worker uses "Draft with AI" to generate FAQ from internal specs | AI-assisted content creation |
| 3 | CMS | Worker edits, sets visibility to `public`, clicks Publish | Public/internal boundary enforcement |
| 4 | Widget | Customer opens portal, clicks chat widget, asks "What do you offer?" | Widget only sees published content |
| 5 | Widget | Widget answers from `public_docs` collection, cites published docs | RAG over curated public corpus |
| 6 | CMS | Worker sees that a public doc is stale, drafts update, republishes | Content lifecycle |
| 7 | Shibboleth | SME reviews an auto-translated FAQ entry in Shibboleth UI | Translation QA pipeline |

---

## Files to Create (revised)

| File | Purpose |
|------|---------|
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | CMS API: CRUD, draft, publish, archive |
| `packages/futuresight-kms/python/km_labels/models.py` | `Document` model with visibility states |
| `packages/futuresight-kms/python/km_labels/database.py` | `documents` table |
| `packages/futuresight-kms/frontend/components/ChatWidget.tsx` | Layer 1 widget (public only) |
| `packages/futuresight-kms/frontend/components/ChatWidget.css` | Widget styles |
| `packages/futuresight-kms/frontend/components/CmsDashboard.tsx` | Layer 2 CMS UI |
| `packages/futuresight-kms/frontend/components/CmsDraftAssistant.tsx` | AI draft generation panel |
| `packages/futuresight-kms/scripts/sync_public_collection.py` | Sync public docs → Qdrant `public_docs` |

---

## Files to Reference (already exist)

| File | Role |
|------|------|
| `services/futuresight-kms/scripts/ingest_docs.py` | Ingests docs into Qdrant — raw internal corpus |
| `orgs/mojomast/ragussy/backend/app/api/rag.py` | RAG search — Layer 3 backend |
| `orgs/mojomast/ragussy/backend/app/api/ragussy.py` | RAG chat — Layer 1 backend (once filtered to `public_docs`) |
| `orgs/mojomast/ragussy/backend/app/api/openai.py` | OpenAI-compat — Layer 3/4 backend |
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Label CRUD — Layer 5 data |
| `packages/futuresight-kms/python/km_labels/routers/export.py` | SFT/RLHF export — downstream of Layer 5 |
| `orgs/octave-commons/shibboleth/ui/src/ChatLab.tsx` | Chat lab — Layer 5 UI reference |
| `orgs/octave-commons/shibboleth/src/promptbench/control_plane/chat_lab.clj` | Chat lab backend — Layer 5 |
| `services/futuresight-kms/config/html/index.html` | Landing page — embed widget, link to CMS |
| `services/portal/index.html` | Portal — embed widget |
| `orgs/mojomast/ragussy/frontend/src/pages-next/DocumentsPage.tsx` | Reference for content list UI patterns |

---

## Status

Draft — specified 2026-04-01. Revised to add CMS boundary layer, scope widget to Layer 1 only, scope Shibboleth to Layer 5.
