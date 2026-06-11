---
uuid: "knoxx-multi-tenant-review-workflow-queue"
title: "Multi-Tenant Review Workflow Queue Enforcement"
status: accepted
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Multi-Tenant Review Workflow Queue Enforcement

> Parent epic: `knoxx-knowledge-ops-multi-tenant-control-plane`
> Points: 3

## Purpose

Replace the single shared review queue with per-tenant review workflow configuration so that review items are scoped to their tenant and cannot bleed across tenant boundaries.

## Scope

- Add a `review_queue_config` field to the `TenantPolicy` type in `src/lib/tenant-types.ts` (or equivalent CLJS namespace) controlling queue routing per tenant.
- Update the review queue read/write paths in backend routes (likely under `src/routes/v1/` or the relevant CLJS tools namespace) to filter by `req.tenantContext.tenant_id`.
- Add or extend integration tests in `src/tests/` to assert that tenant A's review queue items are invisible to tenant B.

## Definition of done

- `TenantPolicy` type includes `review_queue_config` with at least a `tenant_id` scoping field.
- All review queue API responses are filtered to the resolved tenant; cross-tenant queries return empty sets rather than foreign data.
- Integration test covering the cross-tenant isolation scenario passes in CI.

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-tenant-control-plane` on 2026-05-30.
