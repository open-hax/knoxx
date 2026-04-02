# LLM Model Lab -> Knoxx Unified UI Handoff

## Repo + Runtime Context
- LLM Model Lab repo: `/home/mojo/llm-model-lab`
- Knoxx repo: `/home/mojo/knoxx`
- Model Lab API: `:8000`
- Model Lab UI: usually `:5173` (can vary)
- Knoxx API: `:3001`
- Knoxx UI: usually `:5174` (can vary)
- Qdrant: `:6333`

## What Exists Now (Ground Truth)

### Model Lab backend
- OpenAI-compatible endpoints are implemented:
  - `GET /v1/models`
  - `POST /v1/chat/completions`
  - `POST /v1/embeddings`
- `POST /v1/embeddings` supports Knoxx hybrid request flag:
  - request: `knoxx_return_sparse: true`
  - response includes `sparse.indices` + `sparse.values`
- Knoxx proxy routes exist:
  - `GET /api/knoxx/health`
  - `POST /api/knoxx/chat`
  - `POST /api/knoxx/direct`
- Frontend config route exists:
  - `GET /api/config`
- `api/config` now rewrites localhost URLs to request host/scheme for Tailscale/LAN.

Key files:
- `backend/app/api/openai.py`
- `backend/app/api/knoxx.py`
- `backend/app/api/config.py`
- `backend/app/services/embeddings_service.py`
- `backend/app/core/config.py`

### Model Lab frontend
- Current UI includes:
  - Chat Lab
  - Runs
  - provider selector in chat (`Local llama.cpp`, `Knoxx RAG`, `Knoxx Direct`)
  - Knoxx status indicator
  - Knoxx Admin link
- Current app shell and pages are still mostly original Lab UI, not full parity with Knoxx management UX.

Key files:
- `frontend/src/App.tsx`
- `frontend/src/pages/ChatLabPage.tsx`
- `frontend/src/lib/api.ts`

### Knoxx side
- Hybrid retrieval implemented and working:
  - retrieval modes: `dense`, `hybrid`, `hybrid_rerank`
  - dense+sparse indexing and fusion
  - retrieval debug endpoint
  - retrieval stats endpoint
- Direct chat mode exists (`/api/llm/chat`) and UI route exists.
- Upload + auto-ingest + ingestion progress endpoints exist.

Key files:
- `src/routes/chat.ts`
- `src/routes/documents.ts`
- `src/routes/settings.ts`
- `src/services/qdrant.ts`
- `frontend/src/pages/Chat.tsx`
- `frontend/src/pages/Documents.tsx`
- `frontend/src/pages/Settings.tsx`

## Coverage Gap: What Model Lab UI Still Lacks

Model Lab UI does not yet expose most RAG management features that users currently get in Knoxx UI. Missing from Model Lab UX:

1. Document management parity
- list docs
- multi-select indexing
- delete docs
- zip/multi-file upload with auto-ingest
- ingestion status and live progress

2. Vector/RAG ops parity
- collection stats and controls
- retrieval debug compare panel (dense vs hybrid)
- retrieval runtime metrics dashboard

3. Settings parity
- full Knoxx settings form in Model Lab shell (project, providers, RAG params, forum mode, security)
- Model Lab<->Knoxx connection diagnostics panel

4. Operational parity
- admin actions (reindex / full reindex) from Model Lab
- robust UX around long-running ingestion jobs (progress polling, retries, state visibility)

5. Navigation parity
- unified nav in Model Lab for all RAG pages (Chat, Docs, Vectors, Settings, Runs)

## Requested Next Initiative (Priority)

Build a **brand new massively improved Model Lab UI** that fully manages Knoxx from within Model Lab, with reactive design and better UX.

### Hard requirements
1. Keep a bottom-page link to open the **current/legacy UI** so users can switch back.
2. New UI must work for LAN/Tailscale access (no localhost-only links).
3. Include all RAG controls exposed in Knoxx interface (see parity matrix below).

## Implementation Strategy (Recommended)

### Option A (fastest, lowest risk): Model Lab as Super-UI over existing APIs
- Keep Knoxx backend as system-of-record for RAG operations.
- Add/expand Model Lab backend proxy endpoints for all needed Knoxx functions.
- Build new React UI in Model Lab frontend that consumes Model Lab APIs only.
- Benefit: no cross-origin headache, centralized auth/session patterns.

### Option B (slower): direct browser calls to Knoxx from Model Lab
- Not recommended due to CORS/auth complexity and host rewrites.

Use Option A.

## Parity Matrix (Must Reach 100%)

### Chat + Retrieval
- [ ] RAG chat
- [ ] Direct LLM chat
- [ ] Retrieval mode selector (dense/hybrid/hybrid_rerank)
- [ ] Compare retrieval panel
- [ ] Retrieval stats panel (avg/p95, mode counts)

### Documents / Ingestion
- [ ] Upload zip and multiple files
- [ ] Auto-ingest toggle
- [ ] Incremental ingest
- [ ] Full reindex
- [ ] Selective reindex (selected docs)
- [ ] Live ingestion progress (session, percent, file/chunk)
- [ ] Ingestion result summary + errors

### Vector Store / RAG internals
- [ ] Collection health and point counts
- [ ] Search debug tool
- [ ] Hybrid migration helper trigger + guidance

### Settings / Admin
- [ ] Provider settings (LLM + embeddings)
- [ ] API keys/security settings
- [ ] RAG chunking and retrieval knobs
- [ ] Forum mode controls
- [ ] Model Lab integration diagnostics

### Cross-app UX
- [ ] Unified top nav
- [ ] Reactive layouts for desktop/tablet/mobile
- [ ] Persistent user preferences
- [ ] Bottom “Open Legacy UI” link

## New UI Product Requirements

### UX/Design
- Replace current utilitarian layout with a modern operations console style.
- Add intentional information hierarchy:
  - global system status strip
  - nav + context panel
  - page-level action bars
  - feedback surfaces for long-running tasks
- Responsive behavior:
  - desktop: split panels
  - tablet: collapsible side panels
  - mobile: stacked cards with sticky action bar

### Reactivity
- Polling + optimistic UI for ingestion/retrieval telemetry.
- Abortable requests for long operations.
- Progressive loading states and empty states.

### Backward compatibility
- Do not remove existing routes/pages initially.
- Add feature flag or route namespace for new UI, e.g. `/next/*`.
- Keep legacy entry point accessible and linked at page bottom.

## Backend Work Needed in Model Lab (to complete parity)

Add proxy endpoints in `backend/app/api/knoxx.py` (or split modules) for:
- documents list/content/upload/delete
- ingestion trigger + status + progress
- vector debug/search
- settings get/update/test
- chat stats/retrieval debug
- admin reindex actions

Rules:
- Preserve current Knoxx API key usage via backend env (`KNOXX_API_KEY`).
- Normalize response shapes for frontend consumption.
- Ensure host rewrite logic for externally visible URLs.

## Frontend Work Needed in Model Lab

Create a new app shell and pages, suggested structure:
- `frontend/src/pages-next/DashboardPage.tsx`
- `frontend/src/pages-next/ChatPage.tsx`
- `frontend/src/pages-next/DocumentsPage.tsx`
- `frontend/src/pages-next/VectorsPage.tsx`
- `frontend/src/pages-next/SettingsPage.tsx`
- `frontend/src/components-next/*`
- `frontend/src/lib/nextApi.ts`

Legacy fallback link requirement:
- visible at bottom of every new page:
  - text: `Prefer the previous interface? Open Legacy UI`
  - points to legacy route/app.

## Test + Validation Requirements

### Automated
- Add frontend unit tests for critical state flows (upload/ingest/progress/error).
- Add backend tests for proxy route transforms and URL rewrite behavior.
- Add integration smoke script for:
  1) upload fixture docs
  2) ingest
  3) retrieval-debug
  4) chat

### Manual acceptance checklist
- [ ] Tailscale URL access works for all links/buttons
- [ ] All parity matrix items reachable from Model Lab only
- [ ] No need to open Knoxx UI for normal ops
- [ ] Legacy UI link works on every page

## Known Runtime Notes
- `Knoxx Admin` URL now host-aware; still verify with hard refresh after deploy.
- First BGE-M3 run may warm/download and be slower.
- Ensure Knoxx frontend runs on externally reachable host/port when linked.

## Quick Resume Commands
- Start stack helper: `/home/mojo/bootstrap-local-stack.sh start`
- Knoxx health: `curl http://localhost:3001/api/health/detailed`
- Model Lab config: `curl http://localhost:8000/api/config`

## Definition of Done for Next Agent
1. New Model Lab UI ships with full RAG management parity matrix checked off.
2. User can operate end-to-end (upload -> ingest -> debug retrieval -> chat -> tune settings) without opening Knoxx UI.
3. Legacy UI link exists at page bottom and works.
4. Tailscale/LAN host behavior is verified.
5. README updated with screenshots + migration notes from legacy to new UI.
