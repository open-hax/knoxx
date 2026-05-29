---
uuid: "knoxx-knowledge-ops-multi-tenant-control-plane"
title: "Knowledge Ops — Multi-Tenant Control Plane Spec"
status: accepted
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.390Z"
source: "specs/epics/knowledge-ops-multi-tenant-control-plane.md"
points: null
category: epics
---
# Knowledge Ops — Multi-Tenant Control Plane Spec

> Source: `specs/epics/knowledge-ops-multi-tenant-control-plane.md`

> *Every request resolves a tenant. Every object belongs to a tenant. No data crosses boundaries.*

---

## Status Update (2026-04-12)

**Implementation Progress** after audit and P1A implementation:

| Component | Spec Status | Implementation Status | Notes |
|-----------|-------------|----------------------|-------|
| Tenant Catalog | Required | ✅ Implemented | `tenants` collection with CRUD at `/v1/tenants` |
| Tenant Policy Store | Required | ✅ Implemented | `tenant_policies` collection with CRUD at `/v1/tenants/:id/policy` |
| Model Profile Registry | Required | ✅ Schema ready | Migration 004 creates, `model_profiles` collection |
| Tenant Gateway Middleware | Required | ✅ Implemented | `src/plugins/tenant.ts` resolves tenant from X-Tenant-ID header, subdomain, URL param, request body
