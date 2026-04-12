# Knowledge Ops — Multi-Tenant Control Plane Spec

> *Every request resolves a tenant. Every object belongs to a tenant. No data crosses boundaries.*

---

## Purpose

Define the tenant isolation architecture for a multi-tenant domain-aware knowledge management platform that ingests client knowledge, controls PII, translates content, routes uncertain outputs to expert review, and exports labeled examples for model improvement.

---

## Status Update (2026-04-12)

**Current implementation state** after review of openplanner and knoxx codebases:

| Component | Spec Status | Implementation Status | Notes |
|-----------|-------------|----------------------|-------|
| Tenant Catalog | Required | ✅ Implemented | `tenants` collection with CRUD at `/v1/tenants` |
| Tenant Policy Store | Required | ❌ Missing | No `tenant_policies` table, policies embedded in `config` JSONB |
| Model Profile Registry | Required | ❌ Missing | No `model_profiles` table, model selection is per-request |
| Tenant Gateway Middleware | Required | ❌ Missing | Tenant resolved via URL param, not auth/host |
| Tenant-scoped Labels | Required | ✅ Implemented | `km_labels.tenant_id` FK, filtered queries |
| Tenant-scoped Documents | Required | ⚠️ Partial | Uses `project` field, not `tenant_id` |
| Tenant-scoped Translations | Required | ✅ Implemented | `extra.tenant_id` and `extra.org_id` |
| Review Workflow Config | Required | ❌ Missing | Single shared review queue |
| Audit Logging | Required | ❌ Missing | No tenant-scoped audit trail |
| Isolation Ladder | Recommended | ❌ Missing | Shared MongoDB only, no schema-per-tenant |
| Database Profiles | — | ✅ Frontend-only | Knoxx has `qdrantCollection` selection in UI |

---

## Conceptual Model

```
                        ┌───────────────────────────────┐
                        │         CONTROL PLANE         │
                        │                               │
                        │  Tenant Catalog ✅            │
                        │  Tenant Policy Store ❌       │
                        │  Provisioning / Onboarding ⚠️ │
                        │  Model Profile Registry ❌    │
                        │  Review Workflow Config ❌    │
                        │  Audit + Billing Registry ❌  │
                        └──────────────┬────────────────┘
                                       │
                            resolves tenant + policy
                                       │
                    ┌──────────────────▼──────────────────┐
                    │        TENANT GATEWAY / API         │
                    │ auth, host mapping, tenant resolve, │
                    │ RBAC, rate limits, audit logging,   │
                    │ retrieval guardrails, tool policy   │
                    │                                     │
                    │ ❌ Not implemented — tenant_id via  │
                    │    URL param, no middleware         │
                    └───────┬───────────────┬─────────────┘
                            │               │
                 ┌──────────▼──────┐   ┌────▼─────────────┐
                 │ ORCHESTRATION    │   │ REVIEW / LABEL   │
                 │ Openclawssy jobs │   │ queues per tenant│
                 │ ingest/eval/train│   │ + exports        │
                 │                  │   │                  │
                 │ ✅ Events/Docs   │   │ ⚠️ Single shared │
                 └──────────┬───────┘   └────┬─────────────┘
                            │                │
          ┌─────────────────▼────────────────▼─────────────────┐
          │                     DATA PLANE                     │
          │  Doc store | Vector store | Graph store | Logs    │
          │  namespace/schema/collection scoped per tenant     │
          │                                                    │
          │ ⚠️ Shared MongoDB with field filtering only        │
          └─────────────────┬──────────────────────────────────┘
                            │
                     ┌──────▼──────┐
                     │  LLM / MT    │
                     │ generate +   │
                     │ translate    │
                     │              │
                     │ ✅ Proxx/MT  │
                     └──────────────┘
```

**Key principle**: The agent never "figures out" tenancy on its own. The gateway hands the agent a fully resolved, pre-scoped execution context.

**Implementation gap**: Currently, the agent (or frontend) must pass `tenant_id` or `project` explicitly. No gateway middleware exists.

---

## Core Entities

### Tenant (Implemented ✅)

```ts
// Actual implementation in openplanner/src/routes/v1/tenants.ts
Tenant {
  tenant_id: string;      // PK
  name: string;
  domains: string[];      // For host-based resolution (not yet used)
  config: Record<string, any> | null;  // Inline policy config
  created_at?: string;
}
```

**Gap**: No `status` field (trial/active/suspended), no `isolation_mode`, no store refs, no model/profile IDs.

### Supporting Entities

| Entity | Spec | Implementation | Gap |
|--------|------|----------------|-----|
| `TenantPolicy` | Separate table with retention, review thresholds, PII rules | Embedded in `config` JSONB | No schema validation, no versioning |
| `TenantKnowledgeStore` | Vector/doc/graph store refs | ❌ None | All tenants share same MongoDB |
| `TenantModelProfile` | Base model, endpoint, sampling defaults | ❌ None | Model passed per-request |
| `TenantReviewQueue` | Per-tenant queues, assignment rules | ⚠️ `km_labels.tenant_id` filter | No assignment rules, no workflow config |
| `TenantAuditLog` | Append-only log of tenant-scoped actions | ❌ None | No audit trail |

### Document Scoping (Divergence ⚠️)

**Spec**: Documents have `tenant_id`.

**Implementation**: Documents use `project` field for scoping:

```ts
// Actual: openplanner/src/lib/types.ts
DocumentRecord {
  id: string;
  title: string;
  content: string;
  project: string;        // ← Acts as tenant/collection scope
  kind: DocumentKind;
  visibility: DocumentVisibility;
  source?: string;
  sourcePath?: string;
  domain?: string;
  language?: string;
  // ... no tenant_id
}
```

**Resolution**: Gardens use `source_filter.project` to scope document queries. The `project` field effectively serves as a collection/tenant identifier.

### Translation Segments (Implemented ✅)

```ts
// Actual: openplanner/src/lib/types.ts
TranslationSegmentEvent {
  extra: {
    tenant_id: string;    // ✅ Properly scoped
    org_id: string;       // ✅ Hierarchy support
    domain?: string;
    // ...
  };
}
```

---

## Tenant Gateway

### Responsibilities vs Implementation

| Responsibility | Spec | Implementation |
|----------------|------|----------------|
| Resolve `tenant_id` from auth, hostname, API key | Required | ❌ URL param only |
| Resolve `user_id`, `roles`, `entitlements` | Required | ❌ No RBAC |
| Attach `tenant_policy` | Required | ❌ No middleware |
| Enforce rate limits per tenant | Required | ❌ None |
| Reject unscoped retrieval | Required | ⚠️ Frontend passes `project` |
| Write grounding/access logs | Required | ❌ None |

### Resolution Sources

| Source | Spec | Implementation |
|--------|------|----------------|
| Subdomain (`tenant.example.com`) | Planned | ❌ Not implemented |
| JWT/API key claim | Planned | ❌ Not implemented |
| Host header + lookup | Planned | ❌ Not implemented |
| URL param (`/labels/:tenant_id`) | Fallback | ✅ Current approach |
| Request body (`tenant_id` in payload) | Fallback | ✅ Current approach |
| Session context | Planned | ⚠️ Frontend session ID, not tenant |

### Failure Mode

**Spec**: Requests without valid tenant context **fail closed** — no fallback, no default tenant, no anonymous access.

**Implementation**: Labels require `tenant_id` param. Documents use `"devel"` as default project.

---

## Data Plane Isolation

### Isolation Ladder

| Tier | Storage pattern | Spec Recommendation | Implementation |
|------|----------------|---------------------|----------------|
| Shared | Shared app, tenant namespace/schema, strict API filtering | Low-risk and mid-market tenants | ✅ Current state |
| Isolated data | Shared app, separate DB/index/account for tenant | Legal, healthcare, finance | ❌ Not supported |
| Dedicated stamp | Separate deployment stamp + dedicated data plane | Highest-risk or highest-revenue tenants | ❌ Not supported |

### Storage Mapping

| Storage Type | Spec | Implementation |
|--------------|------|----------------|
| **Postgres/MongoDB** | Schema-per-tenant or partition-per-tenant | ❌ Shared `events` collection with `project` filter |
| **Vector DB** | Namespace-per-tenant, metadata filters | ⚠️ `project` filter on queries |
| **Blob/doc store** | Tenant-prefixed buckets | ❌ Single storage |
| **Graph store** | Tenant-labeled subgraph | ⚠️ `project` filter on nodes |

### S3 Path Convention

**Spec**: `kb/{tenant_id}/{domain}/{doc_id}/`

**Implementation**: Not implemented. Documents are stored as events in MongoDB.

---

## API Surface (Current)

| Endpoint | Purpose | Tenant binding | Implementation |
|----------|---------|----------------|----------------|
| `GET /v1/tenants` | List tenants | Admin only | ✅ No auth check |
| `POST /v1/tenants` | Create tenant | Admin only | ✅ No auth check |
| `GET /v1/tenants/:id` | Get tenant | Admin only | ✅ No auth check |
| `DELETE /v1/tenants/:id` | Delete tenant | Admin only | ✅ No auth check |
| `GET /v1/labels/:tenant_id` | List labels | Required | ✅ Filtered by tenant_id |
| `POST /v1/labels` | Create label | Required in body | ✅ Validates tenant exists |
| `GET /v1/documents` | List documents | `project` query param | ✅ Filtered by project |
| `POST /v1/documents` | Upsert document | `project` in body | ✅ Uses project field |
| `GET /v1/lakes` | List lakes (projects) | None | ✅ Aggregates by project |
| `POST /v1/lakes/purge` | Delete by project | `projects[]` in body | ✅ Dangerous, no auth |
| `GET /v1/translations` | List segments | `project` required | ✅ Scoped by project |
| `POST /v1/translations` | Import segments | `tenant_id` in body | ✅ Scoped by tenant_id |
| `GET /v1/gardens` | List gardens | None | ✅ All gardens visible |
| `POST /v1/gardens` | Create garden | `source_filter.project` | ⚠️ Project filter optional |
| `POST /chat` | Chat against KB | `rag_collection` param | ⚠️ Frontend passes collection |

---

## Isolation Invariants

| Invariant | Spec | Current |
|-----------|------|---------|
| Every request resolves to exactly one tenant | Required | ❌ URL param or body, no enforcement |
| Every document, interaction, review, export is tenant-scoped | Required | ⚠️ Uses `project` instead of `tenant_id` |
| Cross-tenant access is impossible by default | Required | ❌ Possible if filter omitted |
| Retrieval never runs an unscoped query | Required | ❌ Unscoped queries return all |
| Audit logs are tenant-scoped | Required | ❌ No audit logs |
| Review queues are per-tenant | Required | ⚠️ Filtered by tenant_id |
| Model profiles are per-tenant | Required | ❌ No model profiles |

---

## Existing Code References

### OpenPlanner Backend

| File | What it has | Gap |
|------|-------------|-----|
| `src/routes/v1/tenants.ts` | CRUD for tenants collection | No status, no isolation_mode, no store refs |
| `src/routes/v1/labels.ts` | `km_labels` with `tenant_id` FK, filtered queries | Tenant from URL param, not middleware |
| `src/routes/v1/documents.ts` | Documents with `project` field for scoping | Uses `project` not `tenant_id` |
| `src/routes/v1/lakes.ts` | Aggregates events by `project` | No tenant concept |
| `src/routes/v1/translations.ts` | Segments with `extra.tenant_id` and `extra.org_id` | ✅ Properly scoped |
| `src/routes/v1/gardens.ts` | Gardens with `source_filter.project` | Optional filter, not enforced |
| `src/routes/v1/cms.ts` | CMS docs with `tenantProject()` helper | Defaults to "devel" |

### Knoxx Frontend

| File | What it has | Gap |
|------|-------------|-----|
| `lib/nextApi.ts` | `listDatabaseProfiles()` with `qdrantCollection` | Collection selection in UI, not tenant |
| `lib/api/runtime.ts` | `rag_collection` param for chat | Per-request collection, not scoped |
| `shell/Shell.tsx` | Status bar shows `collection: devel` | Hardcoded, not from tenant context |

---

## Migration Path

### Phase 1: Tenant Enforcement Middleware (P1A)

1. **Add tenant resolution middleware**
   - Extract `tenant_id` from JWT, API key, or subdomain
   - Attach to `request.tenantContext`
   - Reject requests without valid tenant

2. **Add `tenant_id` to documents**
   - Migration: Add `tenant_id` field to all documents
   - Backfill: `tenant_id = project` for existing docs
   - Update: All document queries require `tenant_id` filter

3. **Create `tenant_policies` collection**
   - Move policy from `config` JSONB to structured schema
   - Add retention_days, review_threshold, pii_rules fields

### Phase 2: Isolation Ladder

4. **Support isolated collections**
   - Add `isolation_mode` to tenant
   - Provision separate MongoDB collections for `isolation_mode: isolated`
   - Route queries based on isolation mode

5. **Add audit logging**
   - Create `audit_log` collection
   - Log all tenant-scoped actions with tenant_id, user_id, action, resource, timestamp

### Phase 3: Model Profiles

6. **Create `model_profiles` collection**
   - Define base_model, endpoint, sampling_params, safety_profile
   - Link tenants to profiles

---

## Summary

**What's Working**:
- Tenant catalog exists
- Labels are tenant-scoped
- Translations are tenant-scoped with org hierarchy
- Lakes/projects provide collection-level aggregation

**Critical Gaps**:
- No tenant gateway middleware
- Documents use `project` not `tenant_id`
- No isolation ladder (shared only)
- No audit logging
- No model profiles
- No RBAC

**Recommended Priority**:
1. Add tenant middleware (blocks P1A-dependent workbench features)
2. Add `tenant_id` to documents with backfill
3. Create `tenant_policies` collection
4. Add audit logging

---

## Sources

- Azure: secure multitenant RAG architecture (API as gatekeeper)
- AWS: multi-tenant generative AI platform (Well-Architected)
- Pinecone: namespace-per-tenant vector DB multitenancy
- Tiger Data: schema-level separation for multi-tenant RAG with PostgreSQL
- Cosmos DB: partition-key-per-tenant patterns

---

## Changelog

- **2026-04-12**: Audited against openplanner and knoxx codebases. Updated to reflect actual implementation state. Added migration path.
- **2026-04-01**: Initial draft from inbox conversation.
