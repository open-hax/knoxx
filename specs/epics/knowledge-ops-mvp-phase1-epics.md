# Knowledge Ops — MVP Phase 1 Epic Spec

> *Tenant onboarding → secure ingestion → tenant-scoped retrieval → expert review → labeled export → audit trail.*

---

## Purpose

Define the MVP scope for the domain-aware knowledge ops platform as a set of epics, user stories, and acceptance criteria. No timelines — sized by relative complexity and level of effort using Fibonacci story points.

---

## Product Statement

A multi-tenant system that ingests client knowledge, detects and controls PII, translates content, routes uncertain outputs to expert review, and produces training-grade feedback data for model improvement.

---

## Core Principles

1. Tenant isolation first
2. PII minimized by default
3. Every action is auditable
4. No training on sensitive data without explicit approval
5. Reviewable outputs are more valuable than raw answers
6. Translation is a workflow, not a side effect

---

## Primary Users

- Tenant admins
- Knowledge managers
- SMEs / expert reviewers
- Internal operators
- Model trainers / evaluators
- End users querying the knowledge base

---

## Sizing Scale

| Points | Meaning |
| :-- | :-- |
| 1 | Tiny — low risk, obvious implementation |
| 2 | Small — simple extension of existing pattern |
| 3 | Moderate — some integration or validation logic |
| 5 | Significant — multiple moving parts, normal uncertainty |
| 8 | Large — cross-cutting work, meaningful unknowns |
| 13 | Too big — must split before commitment |

---

## Epic 1: Tenant Foundation

**Goal**: Make every request, object, and permission tenant-aware.
**Total points**: 21

### Stories

| # | Story | Points | Notes |
| :-- | :-- | :-- | :-- |
| 1.1 | Tenant model and workspace creation | 5 | Core data model, admin bootstrap |
| 1.2 | Tenant context resolution | 5 | Middleware, auth integration, fail-closed |
| 1.3 | Role model and tenant admin bootstrap | 5 | RBAC: admin, reviewer, end user, operator |
| 1.4 | Tenant suspension | 3 | Immediate access stop, session invalidation |
| 1.5 | Isolation test suite | 3 | Cross-tenant read/write denial, runs in CI |

---

## Epic 2: Secure Ingestion

**Goal**: Accept files safely and track them end-to-end.
**Total points**: 31

### Stories

| # | Story | Points | Notes |
| :-- | :-- | :-- | :-- |
| 2.1 | File ingest API | 5 | Upload endpoint, metadata validation |
| 2.2 | Content hashing on ingest | 3 | SHA-256, dedup detection |
| 2.3 | Malware scanning and quarantine | 8 | **Needs split** — scanner integration, async workflow |
| 2.4 | Tenant-scoped object storage layout | 5 | Prefix conventions, quarantine path |
| 2.5 | Ingest status lifecycle | 5 | State machine: pending → accepted → quarantined → failed |
| 2.6 | Source registry | 5 | Source type + identifier per document |

---

## Epic 3: PII Controls

**Goal**: Detect, tag, and constrain sensitive content.
**Total points**: 26

### Stories

| # | Story | Points | Notes |
| :-- | :-- | :-- | :-- |
| 3.1 | PII classification pipeline | 8 | **Needs split** — detection quality, structured output |
| 3.2 | Sensitivity tagging schema | 3 | PII-0 through PII-3 labels |
| 3.3 | Redacted preview generation | 5 | Masking logic for review |
| 3.4 | Log scrubbing | 5 | No raw PII in logs by default |
| 3.5 | Sensitive-content access gating | 5 | Role-based raw access, deny by default |

---

## Epic 4: Retrieval v1

**Goal**: Make tenant-scoped knowledge searchable.
**Total points**: 24

### Stories

| # | Story | Points | Notes |
| :-- | :-- | :-- | :-- |
| 4.1 | Chunking and indexing pipeline | 8 | **Needs split** — parsing variability, metadata preservation |
| 4.2 | Tenant-scoped search API | 5 | Auth integration, relevance ordering |
| 4.3 | Retrieval filters | 2 | Domain, doc type, sensitivity |
| 4.4 | Source citation payload | 2 | Title, source, chunk reference |
| 4.5 | Permission enforcement on retrieval | 5 | Server-side checks, mixed-permission tests |

---

## Epic 5: Audit Trail

**Goal**: Log all security-relevant actions.
**Total points**: 19

### Stories

| # | Story | Points | Notes |
| :-- | :-- | :-- | :-- |
| 5.1 | Audit event schema | 3 | Tenant ID, actor, object, action, timestamp |
| 5.2 | Ingest and retrieval event logging | 8 | **Needs split** — cross-cutting instrumentation |
| 5.3 | Admin action logging | 3 | Tenant lifecycle, role changes, policy overrides |
| 5.4 | Minimal audit query surface | 5 | Filter by tenant, actor, action, date range |

---

## MVP Cut Line

Ship this first:

- Tenant provisioning and isolation
- Ingestion with hashing, virus scanning, and PII tagging
- Retrieval with tenant-scoped search
- Translation with review routing
- Review UI with approve/edit/reject
- Export of labeled review data
- Audit logging for all major actions

---

## Suggested Build Order

1. Tenant foundation
2. Secure ingestion
3. PII controls
4. Retrieval v1
5. Audit trail

---

## Cards Needing Split Before Commitment

These 8-point stories should be decomposed into 3–5 point slices:

- Malware scanning and quarantine
- PII classification pipeline
- Chunking and indexing pipeline
- Ingest and retrieval event logging

---

## Related Specs

- `knowledge-ops-multi-tenant-control-plane.md` — tenant isolation architecture
- `knowledge-ops-pii-handling-protocol.md` — PII lifecycle protocol
- `knowledge-ops-platform-stack-architecture.md` — component boundaries and layers
- `knowledge-ops-shibboleth-lite-labeling.md` — review workflow and label schemas

---

## Existing Code References by Epic

### Epic 1: Tenant Foundation

| Story | Files | State |
|-------|-------|-------|
| 1.1 Tenant model | `packages/futuresight-kms/python/km_labels/database.py` (tenants table), `packages/futuresight-kms/python/km_labels/models.py` (Tenant), `packages/futuresight-kms/src/schema/index.ts` (TenantSchema) | **Partial** — model exists, no policies or model profiles |
| 1.2 Context resolution | — | **Missing** — no middleware, tenant_id is a URL param |
| 1.3 Role model | — | **Missing** — no RBAC at all |
| 1.4 Tenant suspension | `packages/futuresight-kms/python/km_labels/routers/tenants.py` (DELETE endpoint) | **Partial** — hard delete exists, no soft suspend |
| 1.5 Isolation tests | — | **Missing** — no test files exist in futuresight-kms |

### Epic 2: Secure Ingestion

| Story | Files | State |
|-------|-------|-------|
| 2.1 File ingest API | `orgs/mojomast/ragussy/backend/app/api/rag.py` (`POST /ingest/file`) | **Partial** — exists in Ragussy, not tenant-scoped |
| 2.2 Content hashing | `orgs/octave-commons/shibboleth/src/promptbench/util/crypto.clj` (SHA-256) | **Partial** — hash util exists in Shibboleth, not used at ingest |
| 2.3 Malware scan | — | **Missing** — no scanner integration anywhere |
| 2.4 Storage layout | `orgs/mojomast/ragussy/backend/app/api/rag.py` (Qdrant collections) | **Partial** — flat collections, no tenant prefix |
| 2.5 Ingest lifecycle | — | **Missing** — no state machine |
| 2.6 Source registry | — | **Missing** |

### Epic 3: PII Controls

| Story | Files | State |
|-------|-------|-------|
| 3.1 PII classification | — | **Missing** — no detector exists |
| 3.2 Sensitivity tagging | `packages/futuresight-kms/src/schema/index.ts` (PiiLeakageEnum, RiskEnum) | **Partial** — labels exist for review, not for ingest tagging |
| 3.3 Redacted preview | — | **Missing** |
| 3.4 Log scrubbing | — | **Missing** — Ragussy logs contain raw content |
| 3.5 Access gating | — | **Missing** — no role-based access on sensitive content |

### Epic 4: Retrieval v1

| Story | Files | State |
|-------|-------|-------|
| 4.1 Chunking/indexing | `orgs/mojomast/ragussy/backend/app/api/rag.py` (ingest pipeline), `orgs/mojomast/ragussy/backend/app/services/embeddings_service.py` (BGE-M3) | **Partial** — works but not tenant-scoped, no sensitivity metadata |
| 4.2 Search API | `orgs/mojomast/ragussy/backend/app/api/rag.py` (`POST /search`) | **Partial** — semantic search exists, no tenant filter |
| 4.3 Retrieval filters | `orgs/mojomast/ragussy/frontend/src/pages-next/ChatPage.tsx` (retrieval mode selector) | **Partial** — dense/hybrid/rerank modes, no domain/sensitivity filters |
| 4.4 Source citations | `orgs/mojomast/ragussy/frontend/src/pages-next/ChatPage.tsx` (source display) | **Partial** — sources shown in chat, not structured as citation payload |
| 4.5 Permission enforcement | — | **Missing** — no document-level permissions |

### Epic 5: Audit Trail

| Story | Files | State |
|-------|-------|-------|
| 5.1 Audit event schema | — | **Missing** — no audit model exists |
| 5.2 Ingest/retrieval logging | `orgs/mojomast/ragussy/backend/app/services/run_store.py` (JSONL event logs) | **Partial** — run-level logging exists, not security-audit grade |
| 5.3 Admin action logging | — | **Missing** |
| 5.4 Audit query surface | — | **Missing** |

### Cross-cutting: Label CRUD and Export (supports multiple epics)

| File | What it provides |
|------|-----------------|
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Full CRUD: create, read, update, delete, list with filters (tenant, domain, overall, language, pagination) |
| `packages/futuresight-kms/python/km_labels/routers/export.py` | SFT JSONL export, RLHF preference export, manifest/datasheet export |
| `packages/futuresight-kms/python/km_labels/routers/health.py` | `GET /health`, `GET /ready` (DB connectivity check) |
| `packages/futuresight-kms/python/km_labels/app.py` | FastAPI factory with CORS, router mounting at `/api/km-labels`, `/api/tenants`, `/api/export` |
| `packages/futuresight-kms/frontend/components/KnowledgeLabeler.tsx` | Review UI component (hardcoded data, not wired to real endpoints) |
| `packages/futuresight-kms/src/bridge/index.ts` | `KmLabelsClient` — client-side API bridge (defined, never imported) |

### Summary: what's implemented vs what's missing

| Epic | Implemented | Partial | Missing |
|------|:-----------:|:-------:|:-------:|
| 1. Tenant Foundation | — | 2 | 3 |
| 2. Secure Ingestion | — | 2 | 4 |
| 3. PII Controls | — | 1 | 4 |
| 4. Retrieval v1 | — | 4 | 1 |
| 5. Audit Trail | — | 1 | 3 |

---

## Sources

- AWS Well-Architected multi-tenant generative AI guidance
- Azure secure multitenant RAG architecture
- Pinecone namespace-per-tenancy patterns
- NIST Privacy Framework / OWASP sensitive data controls
- HHS/TANF-app security patterns
- Kanban FSM process (workspace canonical)

---

## Status

Draft — derived from inbox conversation on 2026-04-01.
