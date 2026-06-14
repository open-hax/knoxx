---
uuid: "knoxx-rich-action-registry"
title: "Rich Action Registry: Actions as Self-Describing Tools"
status: done
priority: "P1"
labels: ["tasks", "5sp", "action-scope-and-pipeline-collapse"]
created_at: "2026-06-09T00:00:00Z"
source: "docs/design/action-scope-and-pipeline-collapse.md"
points: 5
category: "tasks"
---
# Rich Action Registry: Actions as Self-Describing Tools

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`
> **Blocks:** `knoxx-action-scope-injection` — scope resolution needs rich action metadata

## Context

The action registry (`domain/action/registry.cljs`) is currently just a `defmulti` dispatch table with no metadata. The tool registry (`infra/registry/tools.cljs`) is a hardcoded map with no connection to actions. The resource registry (`domain/registry/resource.cljs`) knows what EDN files exist but nothing about execution.

To achieve the vision where **every action is also a tool** and **scope catalogs are discoverable**, the action registry must become a rich registry that carries:
- Tool metadata (name, description, parameters)
- Event contracts (input event type, output event type)
- Scope declarations (what other actions this action can call)

## Goal

Unify three disconnected systems into one: the action registry becomes the source of truth for execution, tool discovery, and scope resolution.

## Work

### 1. Define Action Metadata Format

Each action carries metadata alongside its implementation:

```clojure
{:action/id :actions/discord-send
 :action/kind :actions/discord-send
 
 ;; Tool surface: if present, this action is exposed as a tool
 :action/tool
 {:name "discord.send"
  :description "Send a message to a Discord channel"
  :parameters [:map
               [:channel_id {:description "Discord channel ID"} :string]
               [:text {:description "Message content"} :string]]
  :risk-level "medium"}
 
 ;; Event contracts: what events this action accepts and emits
 :action/events
 {:input :discord.send/request
  :output :discord.send/complete}
 
 ;; Scope: what other actions are available in this action's context
 :action/scope
 {:actions [:actions/discord-read
            :actions/discord-list-channels]}}
```

### 2. Refactor Action Registry

In `domain/action/registry.cljs`:

- Create `ActionRegistry` protocol or atom-backed registry:
  ```clojure
  (defprotocol IActionRegistry
    (register-action! [registry action-key metadata handler])
    (get-action [registry action-key])             ;; returns {:metadata :handler}
    (get-tool [registry action-key])               ;; returns tool map or nil
    (list-actions [registry])                      ;; returns all registered action keys
    (list-tools [registry])                        ;; returns actions with :action/tool
    (get-event-contract [registry action-key])     ;; returns {:input :output}
    (get-scope-declaration [registry action-key])) ;; returns scope data {:actions [...]}
  ```

  **Note:** `get-scope-declaration` returns raw data. The scope injection task (`knoxx-action-scope-injection`) builds on this with a `resolve-scope` function that returns bound executable functions.

- Migrate existing `defmethod run-action!` implementations to register themselves:
  ```clojure
  ;; Old:
  (defmethod run-action! :actions/hello-world [ctx action] ...)
  
  ;; New:
  (register-action!
    :actions/hello-world
    {:tool {...} :events {...} :scope {...}}
    (fn [ctx action] ...))
  ```

- Preserve backward compatibility: `run-action!` still works as multimethod dispatch, but reads from the registry

### 3. Connect Tool Registry to Action Registry

In `infra/registry/tools.cljs`:

- Replace hardcoded `tool-meta` map with dynamic generation from action registry:
  ```clojure
  (defn get-tool
    [tool-id]
    (when-let [action-key (tool-id->action-key tool-id)]
      (action-registry/get-tool action-key)))
  
  (defn known-tool-ids
    []
    (->> (action-registry/list-tools)
         (map #(get-in % [:tool :name]))
         sort vec))
  ```

- Add `tool-id->action-key` mapping (e.g., `"discord.send"` -> `:actions/discord-send`)

- Keep existing tool IDs working during migration

### 4. Define Event Type Registry

In `domain/action/registry.cljs` or new `domain/event/types.cljs`:

- Map action keys to their input/output event types
- Validate that events dispatched to actions match the declared input type
- Emit output events after action completion

```clojure
{:discord.send/request
 {:event/type :discord.send/request
  :event/payload [:map [:channel_id :string] [:text :string]]}
 
 :discord.send/complete
 {:event/type :discord.send/complete
  :event/payload [:map [:message_id :string] [:channel_id :string]]}}
```

### 5. Update Tool Factory

In `domain/tools.cljs`:

- `create-tool-obj` should accept an action key and build the tool from action metadata
- Tool `execute` function wraps `run-action!` with parameter-to-event conversion:
  ```clojure
  (fn [params]
    (run-action!
      {:event (params->event params action-key)
       :scope (resolve-scope action-key)
       :actor current-actor}
      {:action/kind action-key
       :action/with params}))
  ```

### 6. Migration Strategy

1. **Phase A:** Add rich registry alongside existing multimethod (no breaking changes)
2. **Phase B:** Register all existing actions with metadata
3. **Phase C:** Switch tool registry to read from action registry
4. **Phase D:** Deprecate hardcoded tool map
5. **Phase E:** Add event type validation

## Affected Files

- `backend/src/cljs/knoxx/backend/domain/action/registry.cljs` — major refactor
- `backend/src/cljs/knoxx/backend/infra/registry/tools.cljs` — read from action registry
- `backend/src/cljs/knoxx/backend/domain/tools.cljs` — build tools from actions
- `backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs` — validate event types
- `contracts/actions/hello-world.edn` — add metadata example
- `docs/design/action-registry-intent.md` — update with rich registry design

## Definition of Done

- [ ] Action registry supports metadata + handler registration
- [ ] All existing actions registered with metadata (minimal: empty metadata for migration)
- [ ] Tool registry reads from action registry (fallback to hardcoded map)
- [ ] `list-tools` returns actions with `:action/tool`
- [ ] Event type validation works for action input/output
- [ ] `pnpm -C backend exec shadow-cljs compile test` passes
- [ ] `pnpm -C backend typecheck` passes

## Risks

- Refactoring the action registry is high blast radius — all actions break if registry fails
- Tool IDs may collide during migration — need careful mapping
- Event type validation adds overhead to every action invocation
- Backward compatibility must be preserved for existing `defmethod run-action!` calls

## Notes

- This task is **foundational** for scope injection (Task 1) because `resolve-scope` needs to read action metadata
- This task is **foundational** for the tool-as-action vision because tools must be generated from actions
- Keep the registry in-memory (atom) for fast lookups; reload on contract changes
