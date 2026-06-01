---
uuid: "knoxx-multi-tenant-rate-limit-policy-hook"
title: "Multi-Tenant Rate Limit Policy Hook"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Multi-Tenant Rate Limit Policy Hook

> Parent epic: `knoxx-knowledge-ops-multi-tenant-control-plane`
> Points: 2

## Purpose

Implement per-tenant rate limiting driven by the tenant policy store so that each tenant's request throughput is capped according to its configured policy limits rather than a single global cap.

## Scope

- Add a `rate_limit` field to `TenantPolicy` in `src/lib/tenant-types.ts` (e.g. `{ requests_per_minute: number, burst: number }`).
- Wire a Fastify rate-limit plugin hook (or equivalent middleware in the CLJS backend) that reads `req.tenantContext.policy.rate_limit` after the tenant plugin resolves and applies per-tenant limits.
- Return `429 Too Many Requests` with a `Retry-After` header when the limit is exceeded; ensure no cross-tenant counter leakage (keys namespaced by `tenant_id`).

## Definition of done

- `TenantPolicy` type carries a `rate_limit` struct.
- Requests exceeding the per-tenant limit receive HTTP 429 with a `Retry-After` header; requests from another tenant within the same window are not affected.
- At least one test asserts that tenant-specific rate limits are enforced independently.

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-tenant-control-plane` on 2026-05-30.
