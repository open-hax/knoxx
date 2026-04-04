# Knowledge Ops — Multi-Tenant Control Plane Spec

> *Every request resolves a tenant. Every object belongs to a tenant. No data crosses boundaries.*

---

## Purpose

Define the tenant isolation architecture for a multi-tenant domain-aware knowledge management platform that ingests client knowledge, controls PII, translates content, routes uncertain outputs to expert review, and exports labeled examples for model improvement.

---

## Conceptual Model

```
                        ┌───────────────────────────────┐
                        │         CONTROL PLANE         │
                        │                               │
                        │  Tenant Catalog               │
                        │  Tenant Policy Store          │
                        │  Provisioning / Onboarding    │
                        │  Model Profile Registry       │
                        │  Review Workflow Config       │
                        │  Audit + Billing Registry     │
                        └──────────────┬────────────────┘
                                       │
                            resolves tenant + policy
                                       │
                    ┌──────────────────▼──────────────────┐
                    │        TENANT GATEWAY / API         │
                    │ auth, host mapping, tenant resolve, │
                    │ RBAC, rate limits, audit logging,   │
                    │ retrieval guardrails, tool policy   │
                    └───────┬───────────────┬─────────────┘
                            │               │
                 ┌──────────▼──────┐   ┌────▼─────────────┐
                 │ ORCHESTRATION    │   │ REVIEW / LABEL   │
                 │ Openclawssy jobs │   │ queues per tenant│
                 │ ingest/eval/train│   │ + exports        │
                 └──────────┬───────┘   └────┬─────────────┘
                            │                │
          ┌─────────────────▼────────────────▼─────────────────┐
          │                     DATA PLANE                     │
          │  Doc store | Vector store | Graph store | Logs    │
          │  namespace/schema/collection scoped per tenant     │
          └─────────────────┬──────────────────────────────────┘
                            │
                     ┌──────▼──────┐
                     │  LLM / MT    │
                     │ generate +   │
                     │ translate    │
                     └──────────────┘
```

**Key principle**: The agent never "figures out" tenancy on its own. The gateway hands the agent a fully resolved, pre-scoped execution context.

---

## Core Entities

### Tenant

```ts
Tenant {
  tenant_id: string
  slug: string
  status: "trial" | "active" | "suspended"
  deployment_stamp: string
  isolation_mode: "shared" | "dedicated"
  kb_store_ref: string
  vector_store_ref: string
  graph_store_ref: string
  review_queue_ref: string
  model_profile_id: string
  translation_profile_id: string
  policy_pack_id: string
  billing_account_id: string
}
```

### Supporting Entities

- `TenantPolicy` — retention, review thresholds, PII rules, translation config
- `TenantKnowledgeStore` — vector/doc/graph store refs scoped to tenant
- `TenantModelProfile` — base model, endpoint, sampling defaults, safety profile
- `TenantReviewQueue` — pending review items, assignment rules, export paths
- `TenantAuditLog` — append-only log of all tenant-scoped actions

---

## Tenant Gateway

### Responsibilities

1. Resolve `tenant_id` from auth, hostname, API key, or workspace context
2. Resolve `user_id`, `roles`, and `entitlements`
3. Attach `tenant_policy`
4. Enforce rate limits and quotas per tenant
5. Reject any retrieval call that lacks explicit tenant scope
6. Write grounding/access logs for audits

### Resolution Sources

- Subdomain: `tenant.example.com`
- JWT/API key claim
- Host header + lookup table
- Session context

### Failure Mode

Requests without valid tenant context **fail closed** — no fallback, no default tenant, no anonymous access.

---

## Data Plane Isolation

### Isolation Ladder

| Tier | Storage pattern | Best for |
| :-- | :-- | :-- |
| Shared | Shared app, tenant namespace/schema, strict API filtering | Low-risk and mid-market tenants |
| Isolated data | Shared app, separate DB/index/account for tenant | Legal, healthcare, finance |
| Dedicated stamp | Separate deployment stamp + dedicated data plane | Highest-risk or highest-revenue tenants |

### Storage Mapping

- **Postgres**: schema-per-tenant or row/partition-per-tenant
- **Vector DB**: namespace-per-tenant, metadata filters for user/doc-level trimming
- **Blob/doc store**: tenant-prefixed buckets or containers
- **Graph store**: tenant-labeled subgraph or separate DB

### S3 Path Convention

```
kb/{tenant_id}/{domain}/{doc_id}/
```

Bucket policies, KMS key policies, and lifecycle rules apply per-tenant with zero ambiguity.

---

## Request Flow

1. User hits `tenant.example.com` or sends a tenant-bound API key
2. Gateway resolves `tenant_id` and user claims, loads tenant config from catalog
3. Gateway selects the tenant's retrieval targets: `index=X`, `namespace=tenant_123`, filter rules
4. Orchestrator runs retrieval and generation only with that scoped config
5. Translation layer applies tenant's glossary, target language rules, review thresholds
6. Response is logged with grounding metadata, tenant ID, and any review triggers
7. If confidence or policy checks fail, interaction is copied into tenant's review queue

---

## API Surface (MVP)

| Endpoint | Purpose | Tenant binding |
| :-- | :-- | :-- |
| `POST /chat` | Ask a question against tenant KB | Required |
| `POST /ingest` | Upload a document | Required |
| `POST /review` | Submit a review label | Required |
| `GET /review/queue` | List pending review items | Required |
| `POST /export` | Export labeled training data | Required |
| `GET /audit` | Query audit events | Required + role check |
| `POST /tenant` | Create a tenant | Platform admin only |
| `PATCH /tenant/:id` | Update tenant config | Tenant admin only |

Every endpoint requires `tenant_context` in the request, resolved from auth or host.

---

## Isolation Invariants

1. Every request resolves to exactly one tenant
2. Every document, interaction, review, export, and model profile is tenant-scoped
3. Cross-tenant access is impossible by default (not just "difficult")
4. Retrieval never runs an unscoped query
5. Audit logs are tenant-scoped and tamper-resistant
6. Review queues, exports, and training data are per-tenant
7. Model profiles are per-tenant even if endpoints are shared

---

## MVP Recommendation

- **Control plane**: one `tenants` table, one `tenant_policies` table, one `model_profiles` table
- **Gateway**: one service that resolves tenant, loads policy, blocks unscoped retrieval
- **Vector retrieval**: one shared index cluster, one namespace per tenant
- **Relational metadata**: schema-per-tenant if tenant count is manageable; otherwise shared tables with mandatory `tenant_id` + row-level guards
- **Review/training**: tenant-specific queues and export paths
- **Upgrade path**: support `isolation_mode = dedicated` for customers who need their own data plane later

---

## Existing Code References

### futuresight-kms (tenant model exists, gateway missing)

| File | What it has | Gap |
|------|-------------|-----|
| `packages/futuresight-kms/python/km_labels/database.py` | `tenants` table (tenant_id PK, name, domains JSONB, config JSONB) + `km_labels` table with `tenant_id` FK | No `tenant_policies` or `model_profiles` tables. No row-level security. No isolation ladder |
| `packages/futuresight-kms/python/km_labels/models.py` | `Tenant` (tenant_id, name, domains, config, created_at), `CreateTenantPayload` | Missing `TenantPolicy`, `TenantModelProfile`, `TenantKnowledgeStore` entities |
| `packages/futuresight-kms/python/km_labels/routers/tenants.py` | CRUD: `GET /`, `GET /{id}`, `POST /`, `DELETE /{id}` | No tenant suspension. No policy config. No role checks on endpoints |
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Labels filtered by `tenant_id` in queries (`WHERE tenant_id = $1`) | No middleware-level tenant resolution — tenant_id is a URL param, not an auth-derived context |
| `packages/futuresight-kms/src/schema/index.ts` | `TenantSchema` (tenant_id, name, domains, config, created_at) — Zod | Missing policy, model profile, and isolation mode schemas |
| `packages/futuresight-kms/src/bridge/index.ts` | `KmLabelsClient` with `tenantId` config, all calls pass tenant_id | No gateway — clients talk directly to backend |
| `packages/futuresight-kms/src/types/index.ts` | `KmLabelsClientConfig { baseUrl, apiKey?, tenantId }` | No tenant policy or model profile types |

### Ragussy (no tenancy at all)

| File | What it has | Gap |
|------|-------------|-----|
| `orgs/mojomast/ragussy/backend/app/api/rag.py` | Document ingest (`POST /ingest/text`, `POST /ingest/file`), search (`POST /search`), collection CRUD | All queries are **unscoped** — no tenant_id anywhere. Collections are not tenant-prefixed |
| `orgs/mojomast/ragussy/backend/app/api/openai.py` | `/v1/chat/completions`, `/v1/embeddings` with Bearer auth | Auth is global, not tenant-bound. No per-tenant model profiles |
| `orgs/mojomast/ragussy/backend/app/core/config.py` | `Settings` class with 30+ env vars | No tenant-related config at all |
| `orgs/mojomast/ragussy/frontend/src/pages-next/DocumentsPage.tsx` | DB profile CRUD, multi-file upload, ingestion progress | No tenant selector. No per-tenant storage isolation |

### Shibboleth (no tenancy concept)

| File | What it has | Gap |
|------|-------------|-----|
| `orgs/octave-commons/shibboleth/src/promptbench/pipeline/core.clj` | `def-pipeline` macro — pipeline definition DSL | No tenant filter concept in pipeline definitions |
| `orgs/octave-commons/shibboleth/src/promptbench/control_plane/chat_lab.clj` | Chat labeling lab sessions, labeling, export | Sessions are not tenant-scoped |

---

## Sources

- Azure: secure multitenant RAG architecture (API as gatekeeper)
- AWS: multi-tenant generative AI platform (Well-Architected)
- Pinecone: namespace-per-tenant vector DB multitenancy
- Tiger Data: schema-level separation for multi-tenant RAG with PostgreSQL
- Cosmos DB: partition-key-per-tenant patterns

---

## Status

Draft — derived from inbox conversation on 2026-04-01.
