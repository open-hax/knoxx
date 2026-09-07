---
uuid: "knoxx-tenant-fail-closed-route-guard"
title: "Tenant Context: Fail-Closed Route Guard Middleware"
status: ready
priority: "P2"
labels: tasks, 3sp, has-parent, security, auth
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# Tenant Context: Fail-Closed Route Guard Middleware

> Parent epic: `knoxx-knowledge-ops-mvp-phase1-epics` (Epic 1, Story 1.2)
> GitHub issue: [#175](https://github.com/open-hax/knoxx/issues/175)
> Duplicate intake [#9](https://github.com/open-hax/knoxx/issues/9) is closed in favor of this
> existing canonical projection.

## Problem

`infra/auth/authz.cljs` has `resolve-request-context!` and `with-request-context!`, but protected
routes still use `(when ctx (ensure-permission! ...))`. This is fail-open: when context resolution
returns `nil` (no policy DB configured, missing credentials, or unresolved user), the `when`
short-circuits and the route body can proceed unauthenticated. The original issue named the MCP
tool routes; the live inventory must cover every protected registration/helper rather than freeze
an obsolete file count.

## Goal

Replace the `(when ctx ...)` pattern with a fail-closed equivalent that returns 401 when context is nil on protected routes.

## Scope

- Add one reusable auth-boundary guard that returns the canonical non-enumerating 401 result before
  permission checks or route effects when trusted context is absent.
- Inventory every protected route and helper using `with-request-context!`, `(when ctx ...)`, or an
  equivalent optional-context permission pattern; replace every fail-open protected path.
- Preserve explicitly public routes. A mechanical rewrite must not require auth on health,
  discovery, or other deliberately public surfaces.
- Prove that policy-DB-disabled, missing, invalid, and unresolved identities reach no protected
  repository, tool, filesystem, process, model, or event effect.
- Add a namespace/source regression that fails when a protected route reintroduces an
  optional-context permission check.

## DoD

- The canonical context guard returns 401 with no protected effect when trusted context is nil.
- The live protected-route inventory contains no optional-context permission bypass.
- Public routes remain public and unchanged.
- Focused route tests cover read, mutation, tool/process, and event surfaces; real-server probes
  show missing/invalid context returns 401 rather than 200/500.
- Backend compile/tests and strict changed-surface lint pass with zero warnings.

## Triage note

Accepted at P2 on 2026-05-29 as concrete fail-open-to-fail-closed security hardening.
