---
uuid: "knoxx-multi-tenant-migrations-prod-runbook"
title: "Multi-Tenant Migrations Production Runbook"
status: accepted
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Multi-Tenant Migrations Production Runbook

> Parent epic: `knoxx-knowledge-ops-multi-tenant-control-plane`
> Points: 3

## Purpose

Document and execute the production database migration run that applies migrations 003–007 (tenant policies, model profiles, tenant enhancements, events tenant_id backfill, and audit log) to the live MongoDB instance.

## Scope

- Write a step-by-step runbook in `docs/runbooks/multi-tenant-migrations.md` covering pre-flight checks, migration execution (`node dist/scripts/run-migrations.js`), rollback steps, and post-run verification queries.
- Validate that all five migrations (003_tenant_policies through 007_audit_log) apply cleanly against production and that the `events` backfill from `project` → `tenant_id` completes without data loss.
- Confirm index creation on `tenant_policies`, `model_profiles`, and `audit_log` collections.

## Definition of done

- Runbook file exists at `docs/runbooks/multi-tenant-migrations.md` with pre-flight, execution, verification, and rollback sections.
- All five migrations have been applied to production; migration script exits 0 with no collection errors logged.
- Post-run MongoDB queries confirm `tenant_id` is populated on backfilled events and all required indexes are present.

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-tenant-control-plane` on 2026-05-30.
