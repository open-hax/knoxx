# Knowledge Ops — Platform Stack Architecture Spec

> *Three faces on the same spine: public assistant, internal ops console, expert review workbench.*

---

## Purpose

Define the layered architecture for the domain-aware knowledge ops platform, including component boundaries, naming conventions, data flow, and the relationship between existing OSS tools (Ragussy, Openclawssy, Shibboleth) and the new platform layers.

---

## Conceptual Model

```
┌─────────────────────────────────────────────────────────────┐
│                    PUBLIC ASSISTANT                          │
│  Website chat widget — customer-facing Q&A, FAQ, booking   │
│  Embeddable, tenant-branded, cites sources                  │
└─────────────────────────┬───────────────────────────────────┘
                          │ query + answer
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    KNOWLEDGE CONSOLE                        │
│  Internal ops — ingestion, retrieval diagnostics, search   │
│  Staff-facing: Ragussy RAG + Openclawssy orchestration     │
└─────────────────────────┬───────────────────────────────────┘
                          │ interactions sampled for review
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   REVIEW WORKBENCH                          │
│  Expert review — label, correct, translate, approve        │
│  Shibboleth-lite: DSL + HITL UI + translation QA           │
└─────────────────────────┬───────────────────────────────────┘
                          │ labeled data
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  TRAINING PIPELINE                          │
│  Dataset curation → fine-tuning → eval → model promotion   │
│  Export pipeline, provenance, reproducibility bundles       │
└─────────────────────────────────────────────────────────────┘
```

All layers share the same tenant control plane and audit trail.

---

## Layer Definitions

### 1. Public Assistant

**Purpose**: Customer-facing support / FAQ / "ask the company" interface.

- Web widget / portal where end-users ask questions
- AI sits in front of a curated public knowledge base
- Hands off to human support when confidence is low
- Multi-tenant: each client gets their own branding, content, persona
- Multi-translation: answers in visitor's language with confidence-aware review routing

**Not** Ragussy/Openclawssy; it's the polished "help center + AI chatbot" experience.

**UI components**:
- Searchable knowledge base cards
- Expandable FAQ items
- Assistant chat panel with source citations
- Booking flow with service type and selectable slots
- Language selector with translation confidence indicators
- Bilingual trace view (original + translated side by side)

### 2. Knowledge Console (was "Ragussy Admin")

**Purpose**: Tooling for staff and power users.

- **Ragussy**: local-first RAG + inference + ops console
  - Document DB profiles, ingestion management
  - Retrieval diagnostics and debugging
  - System health, telemetry, run monitoring
- **Openclawssy**: secure agent harness and workflow runner
  - Advanced internal automations and experiments
  - Multi-tool agents (data cleanup, migrations, batch evals)
  - Capability-gated, blast-radius-controlled execution

This is the **control room** — not visible to end users.

### 3. Review Workbench (was "Chat Lab")

**Purpose**: Specialist workstation for curators and SMEs.

- Takes real interactions and presents them for expert review
- Multilingual: shows source/target, lets experts fix translations
- Writes high-quality labeled examples into datasets
- Label dimensions: correctness, groundedness, completeness, tone, risk, PII leakage, translation quality, overall label

**Users**: content teams, translators, compliance/legal, senior SMEs.

### 4. Training Pipeline

**Purpose**: Turn reviewed interactions into model improvement data.

- Export reviewed examples as SFT datasets
- Evaluation dashboards per tenant/domain
- RLHF reward derivation from correctness + risk + tone
- Model registry and promotion gates
- Provenance and reproducibility bundles

---

## Component Mapping to Existing Tools

| New platform concept | Existing tool | Relationship |
| :-- | :-- | :-- |
| RAG engine | Ragussy | Direct use — RAG + inference + ops |
| Agent harness | Openclawssy | Direct use — secure execution governor |
| Review/labeling UI | Shibboleth ChatLab | Reuse UI framework, new schemas |
| Translation pipeline | Shibboleth MT batcher | Reuse mechanics, new content semantics |
| Graph substrate | Fork Tales | Domain routing and clustering |
| HITL labeling DSL | Shibboleth core DSL | Extend with corporate schemas |

---

## Naming Convention

Use industry-standard terms in UI, keep personality as internal codenames:

| UI Label | Internal name |
| :-- | :-- |
| Knowledge Console | Ragussy Admin |
| Inference Console | Model Lab |
| Review Workbench | Chat Lab |
| Evaluation Console | Truth Console |
| Annotation Queue | Manual chat labeling lab |
| Reference Answer | Gold answer |
| Retrieval-Optimized | RAG-friendly preset |
| Evaluation Profile | Deterministic/Eval |

---

## Data Flow

```
Public queries ──┐
                 ├──► Ragussy RAG ──► Answer + Sources ──► Log interaction
Internal queries ─┘         │
                            │ (sampled)
                            ▼
                    Shibboleth Review Queue
                            │
                    Expert labels + corrections
                            │
                            ▼
                    Training Export Pipeline
                            │
                    SFT dataset / Eval set / RLHF rewards
```

---

## Governance Architecture

For compliance-heavy clients (legal, engineering, healthcare):

```
VLAN 10 — Isolated AI Island
  LLM (Worker Model + Output Guard)
  Local RAG database
  NO internet access, NO direct LAN access

VLAN 20 — Jump Box / Deterministic MCP
  Deterministic code only (no LLM)
  ACLs (read/GET only, no POST/DELETE)
  WireGuard access

VLAN 30 — Production Target
  Business APIs (pfREST, Unifi, etc.)
  Only reachable through jump box
```

Selling point: "You own the weights, you own the data, the AI cannot talk to the internet. Period."

---

## Phase 1 Scope

- One tenant onboarding flow
- One document source
- One search endpoint
- One review queue
- One translation path
- One export pipeline

---

## Existing Code References

### Layer 1: Public Assistant — NOT YET IMPLEMENTED

No public-facing widget exists in any repo. The inbox conversation contains mockup descriptions but no code was produced.

### Layer 2: Knowledge Console (Ragussy)

| File | What it implements |
|------|-------------------|
| `orgs/mojomast/ragussy/backend/app/main.py` | FastAPI app entry, lifespan, WebSocket `/ws/stream` |
| `orgs/mojomast/ragussy/backend/app/api/rag.py` | Document ingest, search, collection CRUD against Qdrant |
| `orgs/mojomast/ragussy/backend/app/api/openai.py` | OpenAI-compatible `/v1/chat/completions`, `/v1/embeddings` |
| `orgs/mojomast/ragussy/backend/app/api/server.py` | llama.cpp lifecycle (start/stop/warmup/status) |
| `orgs/mojomast/ragussy/backend/app/api/runs.py` | Run history, export |
| `orgs/mojomast/ragussy/backend/app/services/llama_manager.py` | llama-server process management |
| `orgs/mojomast/ragussy/backend/app/services/embeddings_service.py` | BGE-M3 dense+sparse embeddings |
| `orgs/mojomast/ragussy/frontend/src/pages-next/DashboardPage.tsx` | System overview: health, telemetry, vector stats |
| `orgs/mojomast/ragussy/frontend/src/pages-next/ChatPage.tsx` | RAG/Direct chat with retrieval settings |
| `orgs/mojomast/ragussy/frontend/src/pages-next/DocumentsPage.tsx` | Upload, DB profiles, ingestion progress |
| `orgs/mojomast/ragussy/frontend/src/pages-next/VectorsPage.tsx` | Vector store diagnostics |
| `orgs/mojomast/ragussy/frontend/src/pages-next/SettingsPage.tsx` | Ragussy configuration |

### Layer 3: Review Workbench (Shibboleth + futuresight-kms)

| File | What it implements |
|------|-------------------|
| `orgs/octave-commons/shibboleth/ui/src/ChatLab.tsx` | Manual chat labeling lab: sessions, harm_category/response_class labels, training export preview |
| `orgs/octave-commons/shibboleth/ui/src/App.tsx` | Truth console: pipeline composer, bench launcher, aggregate metrics, truth scatter |
| `orgs/octave-commons/shibboleth/src/promptbench/control_plane/chat_lab.clj` | Chat lab backend: session CRUD, label storage, export writing |
| `orgs/octave-commons/shibboleth/src/promptbench/control_plane/server.clj` | Ring/Jetty HTTP server on `:8788` with reitit routes |
| `packages/futuresight-kms/frontend/components/KnowledgeLabeler.tsx` | Corporate Q&A labeler: 8 label dimensions, gold answer editing, save/skip |
| `packages/futuresight-kms/frontend/components/styles.css` | Labeler styling: 2-column grid, dark mode, responsive |
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Label CRUD backend (tenant-scoped) |
| `packages/futuresight-kms/python/km_labels/routers/export.py` | SFT/RLHF/manifest export |

### Layer 4: Training Pipeline

| File | What it implements |
|------|-------------------|
| `packages/futuresight-kms/python/km_labels/routers/export.py` | `GET /{tenant_id}/sft` (JSONL streaming), `GET /{tenant_id}/rlhf` (preference pairs), `GET /{tenant_id}/manifest` (Shibboleth-style datasheet) |
| `packages/futuresight-kms/src/schema/index.ts` | `SftExampleSchema`, `RlhfPreferenceSchema`, `ExportManifestSchema` |
| `orgs/octave-commons/shibboleth/src/promptbench/eval/runner.clj` | Eval runner (adversarial evals, not corporate) |
| `orgs/octave-commons/shibboleth/src/promptbench/eval/judges.clj` | LLM rubric judges |
| `orgs/octave-commons/shibboleth/src/promptbench/report/bundle.clj` | Reproducibility bundle generation |

### Cross-cutting: Bridge Layer

| File | What it implements |
|------|-------------------|
| `packages/futuresight-kms/src/bridge/index.ts` | `RagussyClient` (health, listModels, chat), `ShibbolethClient` (health, getLabelSchema, listChatSessions), `KmLabelsClient` (full label CRUD + export) |
| `orgs/mojomast/ragussy/backend/app/api/shibboleth.py` | `POST /api/shibboleth/handoff` — exports conversations to Shibboleth (one-directional, exists) |
| `orgs/mojomast/ragussy/frontend/src/pages-next/ChatPage.tsx` | Shibboleth handoff button in chat UI |

### Integration gaps

- `RagussyClient` and `ShibbolethClient` are defined but **never imported or called** anywhere
- `KnowledgeLabeler.tsx` uses **hardcoded placeholder data** — doesn't fetch from Ragussy or the km_labels backend
- `POST /api/shibboleth/handoff` exists in Ragussy but targets Shibboleth's adversarial schema, not futuresight-kms corporate schemas

---

## Sources

- Ragussy: https://github.com/mojomast/ragussy
- Openclawssy: https://github.com/mojomast/openclawremoteussy
- Shibboleth: https://github.com/octave-commons/shibboleth
- Fork Tales: https://github.com/octave-commons/fork_tales
- Enterprise RAG architecture guides
- MLOps / HITL annotation tooling vocabulary

---

## Status

Draft — derived from inbox conversation on 2026-04-01.
