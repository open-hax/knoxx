# Knoxx Resource Architecture

> Status: Implemented (phases 6–12 landed 2026-06-10; see Definition of Done)
> Created: 2026-06-10
> Replaces: `event-trigger-action-runtime.md`, `action-scope-and-pipeline-collapse.md`
> Repo: `packages/agents/knoxx`

## Goal

One execution model. One resource shape. No blended classes.

- **Events** are immutable signals emitted by actors.
- **Triggers** are agreements to act after observing a matching event.
- **Actions** are functions invoked against a context.
- **Agents** are prompt/model/capability specs — they are not triggers, actions, or schedules.
- **Schedules** emit synthetic events; they do not invoke actions.
- **Generators** declare event provenance; they do not subscribe.
- **Sources** are actor-owned driver instances.
- **Stores** are keyed persistence with schemas.

## Resource Shape

Resources live in namespace files, not individual contract files.

```clojure
{:namespace :ussyverse
 :resources
 [{:trigger/id :discord.mentions
   :trigger/actor "discord_automation"
   :trigger/events [:discord.message]
   :trigger/condition (or (conditions/discord.mention event) ...)
   :trigger/with
   {:agent-id "ussyverse_social_replies"
    :streamingBehavior "steer"
    :session-id "ussyverse_social_replies_sticky"
    :task "A Discord event fired..."}

   ;; Anonymous action — no :action/id, not registered, local to this trigger
   :action/scope
   {:actions [:eta-mu/send-message :discord/get-servers :discord/get-messages]
    :filters [:vector/exclude-shared]
    :stores [:ussyverse/replies-agent.observed-messages]}

   :store/id :replies-agent.observed-messages
   :store/schema [:map [:message-id :int] [:author-id :int] [:timestamp :int]]

   :action/fn
   (fn [ctx action]
     (let [{:keys [agent-id task session-id streamingBehavior]} (:with action)
           {:keys [event actor scope]} ctx
           {:keys [discord/get-servers discord/get-messages
                   eta-mu/send-message
                   ussyverse/replies-agent.observed-messages]} scope
           {:keys [content author channel]} event
           recent-messages (await (discord/get-messages channel-id))
           seen-messages (await (ussyverse/replies-agent.observed-messages {:limit 100}))
           unseen-messages (filterv #(not (some #{(:message-id %)} (map :message-id seen-messages)))
                                    recent-messages)]
       ((:eta-mu/send-message scope)
        {:agent-id agent-id
         :session-id session-id
         :task (str task "\n\nRecent unobserved messages:\n" (pr-str unseen-messages))})))}]}
```

### How interpreters read this

- **Trigger interpreter**: reads `:trigger/*` keys. Ignores `:action/*`, `:store/*`.
- **Action interpreter**: reads `:action/*` keys. Ignores `:trigger/*`, `:store/*`.
- **Store interpreter**: reads `:store/*` keys. Ignores `:trigger/*`, `:action/*`.

One resource entry can be all three simultaneously. The namespace resolves identity:
`:trigger/id :discord.mentions` + `:namespace :ussyverse` → `:ussyverse/discord.mentions`

## The Resource Grammar

The manifest format is a language with four rules. They apply to **every**
resource kind, not just triggers/actions/stores.

### 1. Registration: `:K/id` registers a resource of kind K

```clojure
{:agent/id "greeter" ...}      ;; registers :ns/greeter as an agent
{:role/id :curator ...}        ;; registers :ns/curator as a role
{:schedule/id :morning ...}    ;; registers :ns/morning as a schedule
```

Registered resources are discoverable: they appear in catalogs, can be
referenced by qualified id from other resources, and are validated against
their kind's boundary schema. The full kind table lives in
`domain/resources/namespace_file.cljs` (`kind-id-keys`): trigger, action,
store, agent, actor, role, capability, policy, schedule, generator, source,
source-mode, ingest-source, model, model-family, runtime-feature, sub-agent.

### 2. Registration is optional: anonymous facets are owned

`:K/<field>` keys **without** `:K/id` declare an anonymous resource of kind K,
owned by whatever kinds the entry does register. Anonymous resources are never
registered, never discoverable, and live exactly as long as their owner. Kind
K's interpreter reads them in place.

```clojure
{:trigger/id :social-replies     ;; registered: this entry IS a trigger
 :trigger/events [:discord.message]
 :action/fn (fn [ctx action] ...)  ;; anonymous action, owned by the trigger
 :agent/model "glm-5"              ;; anonymous agent facet, owned by the trigger
 :agent/prompts {:system "..."}}
```

`:action/fn` was the first sentence of this language; the rule generalizes.
The loader records ownership as `:resource/anonymous-facets` on every
projected definition so interpreters can adopt facet-by-facet.

### 3. Composite: one entry, many registrations

An entry registers every kind whose `:K/id` it carries. The loader emits one
record per registered kind; each interpreter reads only its own keys.

### 4. References live in the owner's namespace

A reference to another resource is a field of the referring kind —
`:trigger/action`, `:role/capabilities`, `:model/family`, `:schedule/generator`.
A bare `:K/id` always means identity, never a reference. (This is why legacy
model contracts that use `:model-family/id` as a back-reference must rewrite
it as `:model/family` when they migrate to manifests.)

`:data` is deprecated in manifests: every field belongs to a kind's namespace
or it does not belong in the resource.

## What the Code Actually Does Right Now

Grounded in verified code paths as of 2026-06-10.

### Trigger Contract (current schema)

| Field | Status | What Reads It |
|---|---|---|
| `:contract/kind` | ALIVE | Schema dispatch |
| `:contract/id` | ALIVE | `normalize.cljs:34` → `:trigger/id` |
| `:trigger/kind` | ALIVE | `dispatch.cljs:74` (must be `:event`) |
| `:trigger/events` | ALIVE | `normalize.cljs:12`, `dispatch.cljs:57` |
| `:trigger/action` | ALIVE | `normalize.cljs:47`, `registry.cljs:135` |
| `:trigger/agent` | ALIVE | `normalize.cljs:49`, `registry.cljs:141` → merged into `:action/with` |
| `:trigger/actor` | ALIVE | `normalize.cljs:23`, `dispatch.cljs:84` |
| `:trigger/emitter` | ALIVE | `normalize.cljs:27`, `dispatch.cljs:50` |
| `:trigger/listener` | ALIVE | `normalize.cljs:28`, `dispatch.cljs:85` |
| `:trigger/condition` | ALIVE | `normalize.cljs:45`, `dispatch.cljs:66` |
| `:trigger/with` | ALIVE | `normalize.cljs:51`, `registry.cljs:138` → merged into `:action/with` |
| `:enabled` | ALIVE | `normalize.cljs:37`, `dispatch.cljs:73` |
| `:data` | ALIVE (legacy) | `normalize.cljs:43,46,55-58` — fallback for `:filters`, `:condition`, `:task`, `:context` |
| `:trigger/task` | ALIVE (shadow) | `normalize.cljs:52`, `registry.cljs:139` → merged into `:action/with` |
| `:contract/version` | DEAD | Nothing reads it |
| `:trigger/domain` | DEAD | Nothing reads it |
| `:trigger/predicate` | DEAD | Normalizer stores it, dispatch never evaluates it |

### Action Contract (current schema)

| Field | Status | What Reads It |
|---|---|---|
| `:contract/kind` | ALIVE | Schema dispatch |
| `:contract/id` | ALIVE | Loader indexing |
| `:action/id` | ALIVE | Loader, registry result passthrough |
| `:action/kind` | ALIVE | `run-action!` multimethod dispatch key |
| `:action/handler` | ALIVE | Catalog validation, resources API |
| `:action/with` | ALIVE (runtime) | The argument map — `:steps`, `:output`, `:kind`, `:message`, `:agent-id`, `:task` |
| `:contract/version` | DEAD | Nothing reads it |
| `:action/responds-to` | DEAD | Nothing reads it |
| `:action/result` | DEAD | Nothing reads it |
| `:action/scope` (EDN) | DEAD | Registry reads scope from metadata, not EDN |
| `:action/params` | DEAD | Nothing reads it |
| `:data` | DEAD | Nothing reads it from action contracts |

### Agent Contract (current schema)

| Field | Status | What Reads It |
|---|---|---|
| `:contract/id` | ALIVE | Loader, resolve |
| `:contract/kind` | ALIVE | Schema dispatch |
| `:contract/actors` | ALIVE | Actor gating |
| `:enabled` | ALIVE | Resolve, catalog filter |
| `:agent` | ALIVE | Model, thinking, role/roles |
| `:actor` | ALIVE | Capability claims |
| `:prompts` | ALIVE | System/task prompt resolution |
| `:memory` | ALIVE | Passive memory hydration |
| `:sources` | ALIVE | Source composition |
| `:context` | ALIVE | Context policy |
| `:hooks` | DEAD | Zero readers. Always `{}` |
| `:events` | FORBIDDEN | Catalog violation if present on agent |
| `:source-kind` | DEAD | Catalog violation marker only |
| `:source-mode` | DEAD | Catalog violation marker only |
| `:sub-agents` | DEAD | Nothing reads from agent contract |
| `:contract/version` | DEAD | Nothing reads it |
| `:data` | LEGACY | Only `:data :context`, `:data :context-policy`, `:data :tools` as fallbacks |

### Trigger → Action Data Flow (current code)

```
normalize.cljs reads:
  :trigger/action  → becomes :action/kind
  :trigger/with    → merged into :action/with
  :trigger/task    → merged into :action/with as {:task ...}
  :trigger/agent   → merged into :action/with as {:agent-id ...}

action-map (registry.cljs:132-142):
  {:action/id   (name (:trigger/action trigger))
   :action/kind (:trigger/action trigger)
   :action/with (merge (:trigger/with trigger)
                       {:task (:trigger/task trigger)}
                       {:agent-id (:trigger/agent trigger)})}
```

### Action Registry (current code)

Rich registry with metadata. Actions registered via `register-action!`:

```clojure
(register-action!
 :actions/hello-world
 {:action/description "..."
  :action/tool {:name "hello.world" :description "..." :parameters [...] :risk-level "low"}
  :action/events {:input :message/greeting :output :message/send.expectation}
  :action/scope {:actions [:actions/noop]}}
 (fn [ctx action] ...))
```

Multimethod dispatches to registered handlers via `:default` bridge.

### Scope Resolution (current code)

Flat resolution — `resolve-scope` reads `:action/scope` from registry metadata, returns `action-key -> bound-fn` map. No transitive resolution.

## Target Architecture

### Namespace Files

Resources live in namespace files with `:namespace` and `:resources`:

```clojure
{:namespace :ussyverse
 :resources [...]}
```

Identity: `:namespace` + resource's local id → qualified id. Example:
- `:namespace :ussyverse` + `:trigger/id :discord.mentions` → `:ussyverse/discord.mentions`
- Referenced in catalogs as `{:triggers [:ussyverse/discord.mentions]}`

### Composite Resources

A single resource entry can declare trigger, action, and store keys simultaneously. Each interpreter reads only its own keys:

```clojure
{:trigger/id :discord.mentions
 :trigger/events [:discord.message]
 :trigger/with {:agent-id "..." :task "..." :streamingBehavior "steer"}

 :action/scope {:actions [...] :filters [...] :stores [...]}
 :action/fn (fn [ctx action] ...)

 :store/id :replies-agent.observed-messages
 :store/schema [:map [:message-id :int] [:author-id :int] [:timestamp :int]]}
```

### Anonymous Actions

Actions without `:action/id` are anonymous — not registered, not discoverable, local to their containing resource. They're defined via `:action/fn` inline.

### Action Signature

```clojure
(fn [ctx action] ...)
```

Where:
- `ctx` = `{:event :scope :actor :config ...}`
- `action` = `{:action/kind ... :action/with {...}}`
- `:action/with` comes from `:trigger/with` (the generic argument map)

### Scope

`:action/scope` declares what's available to the action:

```clojure
:action/scope
{:actions [:eta-mu/send-message :discord/get-servers]  ;; registered actions
 :filters [:vector/exclude-shared]                       ;; pure functions
 :stores [:ussyverse/replies-agent.observed-messages]}   ;; IStore instances
```

Scope is resolved at dispatch time and injected into `ctx` as `:scope`.

### Stores

Stores are keyed persistence with schemas. Defined via `:store/id` and `:store/schema`, instantiated via `IStore` protocol:

```clojure
(defprotocol IStore
  (-insert [this doc])
  (-find [this query]))
```

### Trigger Arguments

`:trigger/with` is the generic argument map. Everything the action needs goes here:

```clojure
:trigger/with
{:agent-id "ussyverse_social_replies"
 :streamingBehavior "steer"
 :session-id "ussyverse_social_replies_sticky"
 :task "A Discord event fired..."}
```

No `:trigger/agent`, no `:trigger/task` as separate fields. The action receives these via `(:with action)`.

## Migration Path

### What exists now (verified)

- Individual EDN files with `:contract/id` under `contracts/`
- `:trigger/agent` and `:trigger/task` are actively used (merged into `:action/with`)
- `:data` is a legacy fallback path
- Rich action registry with `register-action!`
- Flat scope resolution via `resolve-scope`
- `:actions/run-steps` for pipeline composition
- `:actions/agent-control` for steer/follow-up

### What needs to change

1. **Namespace files** — new resource format with `:namespace` + `:resources` vector
2. **Composite resources** — single entry can be trigger + action + store
3. **`:action/fn`** — inline action implementation
4. **`:trigger/with`** replaces `:trigger/agent` and `:trigger/task`
5. **Store protocol** — `IStore` with `:store/id` and `:store/schema`
6. **Scope expansion** — `:action/scope` includes actions, filters, and stores
7. **Dead field cleanup** — remove `:hooks`, `:events` (on agents), `:contract/version`, etc.

### What stays

- Rich action registry (`register-action!`, `get-tool`, `get-scope-declaration`)
- Multimethod dispatch with `:default` bridge
- Event dispatch → trigger matching → action invocation flow
- `:actions/run-steps` for composed actions
- `:actions/agent-control` for steer/follow-up
- Agent contracts as prompt/model/capability specs

## Knoxx as a Deployment of the Contract Runtime

The end state this grammar serves: **Knoxx is not an application with
contracts bolted on — Knoxx is a deployment of the contract runtime.** A
deployment is a set of namespace manifests plus the code-level pieces the
manifests bind to:

| Layer | What it is | Examples |
|---|---|---|
| **Manifests** | Namespace files declaring resources | `contracts/namespaces/*.edn` |
| **Drivers** | Code that turns the outside world into events / effects | discord gateway, voice windows, eta-mu sessions |
| **Protocols** | Capability seams the runtime binds at deploy time | `IStore`, source driver protocol, condition/filter registries |
| **Libraries** | Pure mechanism with no deployment opinion | loader, schema/law validation, action interpreter, safe-eval |
| **Packages** | The shippable groupings of the above | contract-runtime core, knoxx deployment |

The migration direction: every behavior currently hard-wired into Knoxx either
(a) becomes a manifest entry (registered or anonymous), (b) becomes a driver
behind a protocol, or (c) becomes a library the runtime composes. What remains
in `knoxx.backend` proper should trend toward: the deployment manifest, driver
bindings, and HTTP/WS surface. Knoxx is the prototype deployment of this
system inside OpenPlanner.

Tracked as epic `knoxx-contract-runtime-deployment` (kanban).

## Definition of Done

- [x] Namespace resource loader reads `:namespace` + `:resources`
      (`domain/resources/namespace_file.cljs`, `parse-contract-file-records!`)
- [x] Composite resources parsed by interpreter kind (one record per
      `:trigger/id` / `:action/id` / `:store/id` present; each interpreter
      reads only its own keys)
- [x] `:action/fn` anonymous actions work (`domain/action/anonymous.cljs` —
      fn values pass through; EDN forms interpret against a whitelisted
      pure-function set, fail closed; no `await` — async composition stays in
      registered actions / `:actions/run-steps`)
- [x] `:trigger/with` is the sole argument mechanism (legacy `:trigger/agent`
      / `:trigger/task` fold into it during normalization)
- [x] Store protocol implemented with `IStore` (`MemoryCollection` default;
      `MongoCollection` wraps an injected native handle via `extern/mongo`)
- [x] `:action/scope` includes actions, filters, stores
      (`domain/action/interpreter.cljs` resolves and injects `(:scope ctx)`)
- [x] Dead fields removed from schemas and contract EDN files
- [~] Contracts migrated: live triggers use `:trigger/with`; the composite
      namespace exemplar is `contracts/namespaces/ussyverse.edn`. Remaining
      individual files stay valid — full namespace-format migration is an
      optional follow-up.
- [x] All tests pass (535 tests / 1515 assertions, 0 failures, 2026-06-10)
