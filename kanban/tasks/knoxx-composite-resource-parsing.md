---
uuid: "knoxx-composite-resource-parsing"
title: "Composite Resource Parsing"
status: done
priority: "P1"
labels: ["tasks", "3sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 3
category: "tasks"
---
# Composite Resource Parsing

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

A single resource entry can declare trigger, action, and store keys simultaneously. Each interpreter reads only its own keys.

## Work

1. Define interpreter key sets:
   - Trigger interpreter reads `:trigger/*` keys
   - Action interpreter reads `:action/*` keys
   - Store interpreter reads `:store/*` keys

2. Update `domain/trigger/normalize.cljs` to ignore `:action/*` and `:store/*` keys
3. Update `domain/action/registry.cljs` to ignore `:trigger/*` and `:store/*` keys
4. Add store interpreter for `:store/*` keys
5. Add tests for composite resources

## Definition of Done

- [ ] Single entry can be trigger + action + store
- [ ] Each interpreter reads only its own keys
- [ ] Tests pass

## Risks

- Key namespace collisions
- Interpreter separation clarity
