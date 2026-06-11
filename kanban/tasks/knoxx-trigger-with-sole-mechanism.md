---
uuid: "knoxx-trigger-with-sole-mechanism"
title: ":trigger/with as Sole Argument Mechanism"
status: done
priority: "P1"
labels: ["tasks", "3sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 3
category: "tasks"
---
# :trigger/with as Sole Argument Mechanism

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

Currently `:trigger/agent` and `:trigger/task` are separate fields that get merged into `:action/with`. The target is `:trigger/with` as the sole argument mechanism — everything the action needs goes here.

## Work

1. Update `domain/trigger/normalize.cljs` to read `:trigger/with` as primary
2. Remove `:trigger/agent` and `:trigger/task` merging into `:action/with`
3. Update `domain/action/registry.cljs` `action-map` to pass `:trigger/with` as `:action/with`
4. Migrate existing trigger contracts to use `:trigger/with`:
   - `ussyverse_social_replies_event.edn`: `:trigger/with {:agent-id "..." :task "..." :streamingBehavior "steer"}`
   - Other triggers with `:trigger/agent` or `:trigger/task`
5. Update `start_agent_session.cljs` to read from `:action/with` instead of `:trigger/agent`/`:trigger/task`
6. Add tests

## Definition of Done

- [ ] `:trigger/with` is the sole argument mechanism
- [ ] `:trigger/agent` and `:trigger/task` no longer merged
- [ ] Existing triggers migrated
- [ ] Tests pass

## Risks

- Breaking existing trigger contracts
- Need to migrate all triggers
