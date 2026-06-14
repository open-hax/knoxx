---
uuid: "knoxx-anonymous-actions"
title: "Anonymous Actions via :action/fn"
status: done
priority: "P2"
labels: ["tasks", "3sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 3
category: "tasks"
---
# Anonymous Actions via :action/fn

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

Actions defined inline via `:action/fn` — not registered, not discoverable, local to their containing resource.

## Work

1. Support `:action/fn` in resource entries
2. Anonymous actions are not registered in the action registry
3. The action interpreter executes `:action/fn` directly when present
4. `:action/fn` signature: `(fn [ctx action] ...)`
5. Add tests for anonymous actions

## Definition of Done

- [ ] `:action/fn` inline actions work
- [ ] Not registered, not discoverable
- [ ] Tests pass

## Risks

- Eval/compile strategy for inline fns
- Security implications of inline code
