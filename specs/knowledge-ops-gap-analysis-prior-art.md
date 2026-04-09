# Knowledge Ops — Gap Analysis & Prior Art Research

> *What we're building, who else is doing it, and what we're missing.*

---

## Executive Summary

The futuresight-kms platform sits at the intersection of five established product categories: multi-tenant RAG, agentic CMS, HITL labeling, embeddable chat widgets, and PII-secure knowledge systems. Each has significant prior art. Our architecture is directionally correct but has specific gaps that need addressing before this is production-viable.

---

## 1. Multi-Tenant RAG Architecture

### Prior Art

| Company/Project | Pattern | Key Insight |
|----------------|---------|-------------|
| **Mavik Labs** (2026 guide) | Silo / Pool / Bridge isolation models | Silo = separate index per tenant (enterprise). Pool = shared index + tenant_id filter (SMB). Bridge = hybrid. Our current approach is Pool-only — no Silo option for regulated clients. |
| **AWS Bedrock Knowledge Bases** | JWT-based multi-tenant RAG with OpenSearch | Tenant filter enforced at the retrieval layer, not the application layer. JWT claims propagate directly into vector DB filters. |
| **Pinecone** | Namespace-per-tenant + metadata filtering | "Filter first, then search" — metadata filters run before semantic search, not after. This is the industry consensus. |
| **Azure** | Secure multi-tenant RAG architecture guide | Recommends retrieval-native access control — authorization lives in the retrieval engine, not in application middleware. |
| **GraphRAG multi-tenant** (QuarkAndCode, 2026) | Safe multi-graph isolation | For knowledge graphs: separate subgraphs per tenant with shared embedding space. Relevant if Fork Tales graph layer is added. |

### Gaps in Our Stack

| Gap | Severity | What the Industry Does | What We Do |
|-----|----------|----------------------|------------|
| **No Silo isolation option** | HIGH | Enterprise clients get dedicated indexes/DBs | Pool-only: shared Qdrant with tenant_id metadata |
| **No retrieval-native auth** | HIGH | Vector DB enforces tenant filter, can't be bypassed | Tenant_id is a URL param in the API layer — can be forgotten |
| **No per-tenant encryption keys** | MEDIUM | KMS-per-tenant for enterprise tier | Global encryption, no per-tenant key management |
| **No tenant onboarding automation** | MEDIUM | Programmatic index/key/config creation per tenant | Manual: create tenant row in Postgres, hope for the best |
| **No noisy neighbor protection** | MEDIUM | Rate limits, query cost tracking, usage quotas per tenant | No rate limiting at all |
| **No tenant offboarding (data deletion)** | HIGH | Cascading delete of all tenant data from all stores | Hard DELETE in Postgres, nothing in Qdrant |

### Recommendation

Add a `isolation_mode` field to the Tenant entity (already specced in multi-tenant control plane). Implement retrieval-native tenant filtering by adding `tenant_id` as required metadata on every Qdrant point, and enforce it in the search function, not the API layer. Add a `deleteTenant(tenantId)` function that cascades to Qdrant collections.

---

## 2. Agentic CMS / AI Content Curation

### Prior Art

| Company | Product | Key Insight |
|---------|---------|-------------|
| **Kontent.ai** | "Agentic CMS" (launched 2026) | AI agents work within existing permissions + workflows. Human oversight on every agent action. Multi-agent orchestration. Agents trigger at workflow stages (e.g., "when content enters review, auto-check compliance"). |
| **Contentstack** | Agentic Experience Platform | AI adapts to workflows, not the other way around. No-code agent configuration. |
| **CrawlQ** | "Content ERP" | Transforms disparate files into auditable source of truth. Governed knowledge ingestion. |
| **CMS Critic** (2026 review) | AI CMS category | AI CMS = automation + governance + contextual assistance. Key features: auto-tagging, draft generation, staleness detection, compliance checking. |

### What "Agentic CMS" Means in Practice

1. **AI drafts content** from internal knowledge → human reviews → publishes
2. **AI detects staleness** — flags when published content is outdated based on internal changes
3. **AI enforces compliance** — checks every piece against brand voice, regulatory requirements before publish
4. **AI orchestrates workflows** — content moves through stages (draft → review → publish) with AI actions at each stage
5. **Human oversight is mandatory** — no agent action happens without approval

### Gaps in Our Stack

| Gap | Severity | What the Industry Does | What We Do |
|-----|----------|----------------------|------------|
| **No CMS exists** | CRITICAL | Full content lifecycle management | We have a label CRUD, not a content CRUD |
| **No visibility model** | CRITICAL | Documents have publish states, approval workflows | Our km_labels DB has no Document entity at all |
| **No AI draft generation** | HIGH | LLM generates draft from internal knowledge, human edits | Nothing — no draft pipeline exists |
| **No content staleness detection** | MEDIUM | Monitor internal docs for changes, flag published content | Nothing |
| **No publish workflow** | HIGH | Draft → Review → Publish → Archive state machine | No content lifecycle |
| **No brand voice enforcement** | LOW | Check tone/style before publish | Not in scope for MVP |

### Recommendation

This is the biggest gap. The CMS is the new layer between public (Layer 1) and internal (Layer 3). We need:
1. A `documents` table with `visibility` states (`internal` → `review` → `public` → `archived`)
2. A `POST /api/cms/draft` endpoint that uses Ragussy to generate drafts from internal docs
3. A `POST /api/cms/publish/{id}` endpoint that syncs the doc to the `public_docs` Qdrant collection
4. A CMS UI (content library + draft assistant)

This was already identified in the platform layer spec. Prior art confirms it's the right call.

---

## 3. HITL Labeling Platforms

### Prior Art

| Company | Product | Key Insight |
|---------|---------|-------------|
| **Encord** | Annotation & Labeling Platform ($110M funded) | AI-assisted HITL workflows. Multi-modal (image, video, text, DICOM). Customizable ontologies. Multi-stage review workflows. Integrates GPT-5, Gemini, custom models for auto-labeling. |
| **Surge AI** | RLHF / SFT data labeling | Specialized in LLM fine-tuning data. SFT, RLHF, human evaluation. |
| **Label Studio** | Open source annotation | Most-used open-source HITL tool. Customizable labeling interfaces, ML backend integration. |
| **SageMaker Ground Truth** | AWS managed labeling | Built-in workforce management, active learning, annotation consolidation. |
| **Toloka** | HITL + LLM tutorial (COLING 2025) | LLM pre-labels → humans correct → active learning selects most important examples. |
| **Welo Data** | Enterprise AI training data | Full-lifecycle data partner: training through deployment, multi-language. |

### What HITL Platforms Get Right

1. **Ontology-first**: Define label schema before any labeling starts
2. **Multi-stage review**: Label → Review → Audit → Export pipeline
3. **Annotator quality tracking**: Per-annotator accuracy, agreement scores, disagreement resolution
4. **Active learning**: Select which examples to label based on model uncertainty
5. **Provenance**: Every label traces back to annotator, timestamp, version
6. **Export formats**: SFT JSONL, RLHF preference pairs, evaluation sets

### Gaps in Our Stack

| Gap | Severity | What the Industry Does | What We Do |
|-----|----------|----------------------|------------|
| **No annotator quality tracking** | HIGH | Per-user accuracy, inter-annotator agreement, disagreement queues | Labels store `labeler_id` but no quality metrics |
| **No active learning** | MEDIUM | Select uncertain examples for labeling priority | Random queue order |
| **No multi-stage review** | MEDIUM | Label → Review → Audit pipeline | Single-pass: label → done |
| **No disagreement resolution** | MEDIUM | When 2 annotators disagree, escalate to senior | No multi-annotator support at all |
| **No ontology management** | HIGH | Define, version, and evolve label schemas | Schema is hardcoded in Zod/Pydantic, no runtime management |
| **Shibboleth has this but it's adversarial** | HIGH | Shibboleth's chat lab has session management, labeling, export | Wrong domain semantics (harm_categories, not corporate QA) |

### Recommendation

Shibboleth's chat lab infrastructure is the closest to a production HITL platform. The gap is semantic: it needs corporate QA schemas instead of adversarial schemas. The spec for Shibboleth-lite labeling covers this. Add annotator quality tracking as a Phase 2 feature.

---

## 4. Embeddable Chat Widget Libraries

### Prior Art

| Library | Pattern | Key Insight |
|---------|---------|-------------|
| **Helix Chat Widget** (`@helixml/chat-widget`) | React component + browser-side embed. Config: `url`, `model`, `bearerToken`, theme customization. | Clean API: one component, pluggable backend, theme overrides. Available as both CDN script and React component. |
| **GENChat** | Multi-tenant embeddable widget with pluggable AI backends | "Governed conversational layer: deterministic orchestration first, LLMs as one component." Domain allowlisting, CORS, iframe sandboxing. Version evolution: v1 pure logic → v2 LLM for intent → v3 constrained conversation. |
| **Microsoft Omnichannel Chat Widget** (`@microsoft/omnichannel-chat-widget`) | Enterprise-grade, Power Platform integration | Full-featured but heavy. Designed for Dynamics 365 ecosystem. |
| **Ably Chat React Components** (`@ably/chat-react-ui-components`) | Composable chat components | Real-time-first (Ably pub/sub). Configurable message types, presence, reactions. |
| **Blumessage** (`@blumessage/react-chat`) | Floating button + theming + API integration | Simple: floating button, expandable panel, API key auth. |
| **Vectara React Chatbot** (`vectara/react-chatbot`) | RAG-specific chat widget | Purpose-built for Vectara RAG backend. Apache 2.0. 91 stars. |
| **EmbeddedChat** (`@embeddedchat/react`) | Rocket.Chat embedding | Wraps existing chat platform in React components. |
| **LiveChat Widget** (`@livechat/widget-react`) | Customer support widget | Mature: agent assignment, chat history, analytics. |

### What Good Chat Widget Libraries Have

1. **Dual distribution**: React component + standalone browser script
2. **Pluggable transport**: REST, WebSocket, SSE — configurable, not hardcoded
3. **Theme system**: CSS custom properties + override objects
4. **Session persistence**: localStorage save/restore across page navigation
5. **Multi-tenant config**: One widget codebase, per-tenant branding/endpoint
6. **Human escalation**: When AI can't answer, hand off to human agent
7. **Domain allowlisting**: CORS + iframe sandbox for security
8. **Analytics**: Track conversations, resolution rates, escalation rates

### Gaps in Our Chat UI Library Spec

| Gap | Severity | What the Industry Does | Our Spec Does |
|-----|----------|----------------------|---------------|
| **No browser-side embed** | MEDIUM | CDN script tag for non-React sites | React-only component |
| **No human escalation** | HIGH | "Talk to a human" button when confidence is low | Not mentioned |
| **No session persistence** | MEDIUM | localStorage across page navigation | `utils/persist.ts` exists but not wired |
| **No domain allowlisting** | HIGH | CORS + allowed domains per tenant | Not mentioned |
| **No analytics hooks** | LOW | Track conversations, resolution rates | `onSend` interceptor exists but no analytics |
| **GENChat's governance-first approach is missing** | MEDIUM | "Chatbots should never be directly connected to an LLM" — orchestration layer with routing, caching, validation | Our widget talks directly to Ragussy with no orchestration layer |

### Recommendation

Add to the chat-ui library spec: (1) browser-side embed via CDN script tag, (2) human escalation protocol ("talk to a human" button), (3) session persistence via localStorage, (4) domain allowlisting. GENChat's "governance-first" approach is worth adopting — the widget shouldn't talk directly to the LLM, it should talk to an orchestration layer that decides routing, caching, and fallback.

---

## 5. PII Handling in RAG Systems

### Prior Art

| Source | Pattern | Key Insight |
|--------|---------|-------------|
| **Kiteworks** (2026 RAG security guide) | Retrieval-native access control + 8-layer security | Document-level access controls (RBAC + ABAC + document-scoped policies). Ingestion hygiene (source vetting, adversarial scans, metadata tagging). Retrieval-time authorization (filter before search). Runtime monitoring (PII redaction in outputs). Immutable audit trails. |
| **Satadru** (2026 Medium) | Two patterns: Redaction at storage OR RBAC at retrieval | Pattern 1: Zero-trust — redact PII before embedding (Snowflake `AI_REDACT`, Databricks `ai_mask`). Pattern 2: Role-based — keep PII, filter at retrieval by user role + metadata. "Filter first, then search." |
| **Private AI** | RAG privacy guide | PII detection → redaction/tokenization before embedding. Re-identification only for authorized users. |
| **SANS SEC495** | Building & Securing RAG course | Formal training on RAG security: ingestion, retrieval, MLOps, runtime. |
| **Ragwalla** | Enterprise AI security guide | 73% of enterprises have AI data security gaps. Key: access control, audit logging, compliance (GDPR/HIPAA). |
| **Elastic** | PII protection in RAG with Elasticsearch | Named entity recognition at ingest + metadata filtering at retrieval. |

### The Two Schools of Thought

**School 1: Zero-Trust Redaction**
- Detect and redact PII at ingestion, before embedding
- PII never enters the vector store
- Simple, safe, but limits usefulness (no personalization, no case-specific answers)

**School 2: Role-Based Access**
- Keep PII, tag with metadata (`contains_pii: true`, `access_level: admin/user`)
- Filter at retrieval based on user role
- More useful but more complex — requires robust auth propagation

**Industry consensus**: Use both. Redact by default, allow controlled access with RBAC metadata for cases where PII is necessary.

### Gaps in Our PII Spec

| Gap | Severity | What the Industry Does | Our Spec Does |
|-----|----------|----------------------|---------------|
| **No PII detection at ingest** | CRITICAL | NER/regex/ML detection before embedding | Nothing — Ragussy ingests raw content into Qdrant |
| **No metadata filtering at retrieval** | HIGH | `contains_pii`, `access_level` metadata on every chunk | No metadata beyond `tenant_id` |
| **No output redaction** | HIGH | Scan LLM responses for PII before returning to user | Nothing |
| **No retrieval-native auth** | CRITICAL | Vector DB enforces access filters | Auth is at API layer only |
| **No audit trail for PII access** | HIGH | Log every access to PII-bearing documents | Run-level logging only, no per-document tracking |
| **No WORM/versioning for compliance** | MEDIUM | Immutable storage for regulatory docs | No document versioning |

### Recommendation

Implement the two-pattern approach from Satadru: (1) PII detection at ingest with redaction option, (2) metadata-based filtering at retrieval. Add `contains_pii` and `access_level` metadata to every Qdrant chunk. Add output scanning before returning responses. The audit trail is already specced (Epic 5) but needs PII-specific events.

---

## 6. Cross-Cutting Gaps (Not in Any Single Category)

### What We're Missing That Doesn't Fit Neatly

| Gap | Severity | Description |
|-----|----------|-------------|
| **No content versioning** | HIGH | Published docs have no version history. Can't roll back. Can't diff. Can't track who changed what. |
| **No A/B testing for responses** | LOW | "Does the widget answer better with model A or model B?" No experiment framework. |
| **No feedback loop from widget to CMS** | HIGH | Widget conversations aren't sampled for CMS review. The public-facing layer produces no training signal. |
| **No search analytics** | MEDIUM | "What are customers asking? What topics have no good answers?" No query logging or gap analysis. |
| **No content-to-training pipeline** | HIGH | Labeled data from km_labels doesn't flow back into fine-tuning. The export endpoints exist but nothing consumes them. |
| **No SLA/uptime monitoring** | MEDIUM | No health checks, no alerting, no fallback when services are down. |
| **No cost tracking per tenant** | MEDIUM | No token counting, no per-tenant billing, no usage quotas. |

---

## Summary: Priority Order for Closing Gaps

| Priority | Gap | Spec That Covers It |
|----------|-----|---------------------|
| P0 | CMS (visibility model, publish workflow, draft generation) | `knowledge-ops-chat-widget-layers.md` (Layer 2) |
| P0 | PII detection at ingest | `knowledge-ops-pii-handling-protocol.md` |
| P0 | Retrieval-native tenant filtering | `knowledge-ops-multi-tenant-control-plane.md` |
| P1 | Widget → CMS feedback loop (sample conversations for review) | New spec needed |
| P1 | Tenant offboarding (cascading delete) | `knowledge-ops-multi-tenant-control-plane.md` |
| P1 | Shibboleth-lite schemas (corporate QA instead of adversarial) | `knowledge-ops-shibboleth-lite-labeling.md` |
| P2 | Shared chat-ui library | `knowledge-ops-chat-ui-library.md` |
| P2 | Content versioning | CMS spec |
| P2 | Annotator quality tracking | Labeling spec |
| P3 | Cost tracking per tenant | New spec needed |
| P3 | A/B testing framework | New spec needed |

---

## Sources

- Mavik Labs: Multi-Tenant RAG in 2026 (maviklabs.com)
- AWS: Multi-tenant RAG with JWT (aws.amazon.com)
- Kiteworks: RAG Pipeline Security Best Practices 2026 (kiteworks.com)
- Satadru: Architecture pattern to protect sensitive data in RAG (Medium, Jan 2026)
- Kontent.ai: Agentic CMS (kontent.ai)
- Encord: HITL Annotation & Labeling Platform (encord.com)
- Helix: Chat Widget (docs.helix.ml)
- GENChat: Multi-tenant embeddable chat (gensoftware.dev)
- Uplatz: Multi-tenancy Patterns in Vector Databases (uplatz.com)
- QuarkAndCode: Multi-tenant GraphRAG (Medium, Mar 2026)

---

## Status

Research complete — 2026-04-01.
