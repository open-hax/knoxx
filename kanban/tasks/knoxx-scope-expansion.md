---
uuid: "knoxx-scope-expansion"
title: "Scope Expansion: Actions, Filters, and Stores"
status: done
priority: "P1"
labels: ["tasks", "3sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 3
category: "tasks"
---
# Scope Expansion: Actions, Filters, and Stores

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

Currently `:action/scope` only includes actions. The target is actions, filters, AND stores.

## Work

1. Update `resolve-scope` to build scope map with three categories:
   - `:actions` — bound action fns
   - `:filters` — pure functions (e.g., `:vector/exclude-shared`)
   - `:stores` — `IStore` instances

2. Update scope resolution to:
   - Resolve `:actions` from action registry (existing)
   - Resolve `:filters` from a filter registry (new)
   - Resolve `:stores` from store registry (depends on store protocol task)

3. Add filter registry for pure functions
4. Update action handlers to access filters and stores from scope
5. Add tests

## Definition of Done

- [ ] `:action/scope` includes actions, filters, stores
- [ ] Filter registry exists
- [ ] Stores available in scope
- [ ] Tests pass

## Risks

- Filter registry design
- Store integration dependency
