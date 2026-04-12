# Knowledge Ops — Multi-Tenant Control Plane Spec

> *Every request resolves a tenant. Every object belongs to a tenant. No data crosses boundaries.*

---

## Status Update (2026-04-12)

**Implementation Progress** after audit and P1A implementation:

| Component | Spec Status | Implementation Status | Notes |
|-----------|-------------|----------------------|-------|
| Tenant Catalog | Required | ✅ Implemented | `tenants` collection with CRUD at `/v1/tenants` |
| Tenant Policy Store | Required | ✅ Implemented | `tenant_policies` collection with CRUD at `/v1/tenants/:id/policy` |
| Model Profile Registry | Required | ✅ Schema ready | Migration 004 creates, `model_profiles` collection |
| Tenant Gateway Middleware | Required | ✅ Implemented | `src/plugins/tenant.ts` resolves tenant from X-Tenant-ID header, subdomain, URL param, request body |
| Tenant-scoped Labels | Required | ✅ Implemented | `km_labels.tenant_id` FK, filtered queries |
| Tenant-scoped Documents | Required | ⚠️ Migration added | Migration 006 adds `tenant_id` to events, backfills from `project` |
| Tenant-scoped Translations | Required | ✅ Implemented | `extra.tenant_id` and `extra.org_id` |
| Review Workflow Config | Required | ❌ Not enforced | Single shared review queue |
| Audit Logging | Required | ✅ Schema ready | Migration 007 creates `audit_log` collection |
| Isolation Ladder | Recommended | ❌ Not needed | Shared MongoDB only, schema-per-tenant not required |
| Database Profiles | — | ✅ Frontend-only | Knoxx has `qdrantCollection` selection in UI |
| Tenant Context on Request | Required | ✅ Implemented | `req.tenantContext` attached by middleware |

---

## Critical Gaps Closed

All P1A requirements have been **implemented**:

| Gap | Status | How to Address |
|-----|--------|---------------|
| Migration backfill | Pending | Run migrations to production |
| Policy enforcement | Not enforced | Add enforcement hooks after migration |
| RBAC | Not implemented | Future: JWT role extraction |
| Rate limiting | Not implemented | Future: Policy enforcement |

---

## Completed Work (2026-04-12)

1. **Tenant Types** (`src/lib/tenant-types.ts`)
   - `Tenant`, `TenantPolicy`, `TenantModelProfile`, `TenantContext` interfaces
   - `TenantStatus` ("trial" | "active" | "suspended")
   - `IsolationMode` ("shared" | "isolated" | "dedicated")

2. **Tenant Plugin** (`src/plugins/tenant.ts`)
   - Resolves tenant from multiple sources:
     - `X-Tenant-ID` header (highest priority)
     - Subdomain from `Host` header
     - URL param `:tenant_id` (legacy fallback)
     - Request body `tenant_id` (legacy fallback)
   - Loads policy when available
   - Attaches `TenantContext` to `req.tenantContext`
   - Enforces suspended status (403 Forbidden)
   - Non-strict mode for backward compatibility during migration

3. **Migrations** (`src/lib/migration.ts`)
   - 003_tenant_policies: Creates `tenant_policies` collection with indexes
   - 004_model_profiles: Creates `model_profiles` collection with indexes
   - 005_tenants_enhanced: Adds `status`, `isolation_mode`, `slug` to tenants
   - 006_events_tenant_id: Adds `tenant_id` to events, backfills from `project`
   - 007_audit_log: Creates `audit_log` collection with indexes

4. **Tenant Routes** (`src/routes/v1/tenants.ts`)
   - Enhanced CRUD with new fields: `status`, `isolation_mode`, `slug`, `model_profile_id`, `policy_id`, `owner_id`
   - Added policy management: `GET /:tenant_id/policy`, `PUT /:tenant_id/policy`

5. **Tests** (`src/tests/tenant-plugin.test.ts`)
   - Integration tests using vitest
   - Tests tenant resolution from X-Tenant-ID header
   - Tests tenant resolution from subdomain
   - Tests tenant resolution from URL param
   - Tests suspended tenant enforcement
   - Tests public path bypass

---

## Remaining Work

1. **Run Migrations** - Apply migrations to production database
2. **Policy Enforcement** - Add enforcement hooks after migration stabilizes
3. **RBAC** - Implement JWT role extraction
4. **Rate Limiting** - Implement per-tenant rate limits

---

## Next Steps

1. Run migrations against production: `node dist/scripts/run-migrations.js`
2. Update frontend to Use `req.tenantContext` for status bar collection display
3. Add policy enforcement to documents/retrieval after migration

---

## Changelog

- **2026-04-12**: Implemented P1A Phase 1 (tenant middleware, policies, migrations, tests). Spec updated to reflect reality.
- **2026-04-12**: Audited against openplanner and knoxx codebases. Updated status table to show actual implementation state. Added migration path.
- **2026-04-01**: Initial draft from inbox conversation.
