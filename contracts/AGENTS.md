# Knoxx Resources — EDN Authoring Guide

This document is the canonical reference for writing correct Knoxx resource EDN.
Read it before creating or editing any EDN file under `contracts/`.

---

## Resource System Overview

EDN files under `contracts/` describe resources. The directory name is legacy;
the files themselves are not "contract objects." They must satisfy boundary
contracts: Malli schemas, admissibility checks, and policy rules.

Resources live in `contracts/<class>/<id>.edn` and are currently loaded by the
legacy contract loader through the resource loader facade.

The resource identity is declared inside the EDN body (`:contract/id`, `:actor/id`,
`:role/id`, `:cap/id`, etc.). File stems should still match the body identity for
human navigation and write tooling, but runtime truth comes from the parsed EDN
body, not from directory placement alone.

---

## Directory Layout

```
contracts/
  agents/         Agent resources — executable prompt/model/capability specs only
  actors/         Actor resources — principals: users, services, system actors
  roles/          Role resources — role slug to capabilities/permissions
  capabilities/   Capability resources — capability id to tool/action surfaces
  policies/       Policy resources — invariants, guardrails, and denials
  generators/     Generator resources — event-producing provenance/adapters
  schedules/      Schedule resources — temporal rules that emit synthetic events
  actions/        Action resources — registered behavior and schemas
  pipelines/      Pipeline resources — ordered action sequences
  triggers/       Trigger resources — actor agreements to act after observing events
  source_modes/   Legacy source-mode resources during migration
  sources/        Source resources — actor-owned driver instances or context providers
  models/         Model resources — individual model metadata
  model_families/ Model family resources — shared properties across model variants
  ensemble/       Grouped agent resources for multi-agent sessions
```

---

## ID and Namespace Conventions

Resource ids follow a namespaced convention that mirrors the filesystem.

| Resource class | Id form | Example |
|---|---|---|
| agents | plain string, snake_case | `"my_agent"` |
| actors | plain string, snake_case | `"chat_primary"` |
| roles | keyword `:role/<kebab-slug>` | `:role/contract-librarian` |
| capabilities | keyword `:cap/<kebab-slug>` | `:cap/contract-write` |
| policies | plain string, snake_case | `"no_nrepl"` |
| source_modes | keyword `:source-mode/<kebab-slug>` plus plain `:contract/id` | `:source-mode/discord-synthesis` |
| sources | keyword `:source/<kebab-slug>` plus plain `:contract/id` | `:source/openplanner-memory` |
| models | plain string, snake_case | `"glm_5"` |

**Namespace is allowed and expected on role and capability ids.**
`:role/contract-librarian` normalises to slug `contract-librarian` which maps to file `roles/contract_librarian.edn`.
`:cap/read-directory` normalises to slug `cap_read_directory` which maps to `capabilities/cap_read_directory.edn`.

File names normally use `snake_case` except compatibility files that already use hyphens. Keywords inside resources use `kebab-case`.
The runtime bridges both forms. **Never mix them within the same resource.**

---

## The Duplicate Slug Trap

The runtime resolves dashes and underscores interchangeably when looking up role files.
However it does **not** deduplicate at the string level before they propagate into the
resolved context — it only calls `distinct` after `concat`. So if one site emits
`"contract-librarian"` and another emits `"contract_librarian"`, both survive and produce
a visible duplicate in `role-slugs-from-contract`.

Root cause: an actor uses `:actor/roles [:role/contract-librarian]` (kebab keyword)
and the agent uses `:agent {:role :contract_librarian}` (bare snake keyword).
Fix: use `:role/contract-librarian` in both files.

---

## Agent Resource (agents/<id>.edn)

{:contract/id    "my_agent"         ; must match filename stem
 :contract/kind  :agent
 :contract/version 1
 :enabled        true
 ; Actors allowed to invoke this agent (actor id strings)
 :contract/actors ["chat_primary" "knoxx_default"]

 ; Capabilities granted ON TOP of the actor's baseline role.
 ; Always :cap/<kebab-slug> keywords.
 :actor
 {:capabilities [:cap/read :cap/websearch :cap/memory]}

 ; Role this agent runs as.  MUST be :role/<kebab-slug>.  Not a bare keyword.
 :agent
 {:role   :role/knowledge-worker    ; CORRECT
  :model  "glm-5"
  :thinking :off}                  ; :off | :medium | :high

 :prompts {:system "You are ..."}

 ; Optional context overflow policy for long-running/sticky sessions.
 ; Preserves system messages by default, then keeps the newest body messages.
 :context {:max-messages 40          ; max non-system transcript messages to retain
           :max-chars 80000          ; approximate text budget across retained body messages
           :preserve-system true}    ; set false only if the system prompt may be pruned

 :data    {}                        ; static runtime config only; not mutable memory/state
 :hooks   {:before {} :after {}}}

Roles are **composable**. Use `:role` for a single role or `:roles` for a vector of roles. Both feed into `agent-role-claims` which merges them. Tools, system prompts, and task prompts are composed across ALL declared roles.

### Source modes

Legacy source-mode resources are transitional prompt-context adapters. New event runtimes should use source resources for driver-backed event generators and generator resources for pure EDN/schedule provenance; only maintain source-mode references for existing source-mode consumers. Prefer explicit source refs over opaque bare modes:

```clojure
:sources [:source/discord-synthesis]
```

The legacy `:source-mode/discord-synthesis` resource maps to runtime `:source/mode :template-synthesize`, which derives `ctx.source.messages` from explicit source filters and renders it through prompt templates. It is paired with `:role/discord-source-synthesizer`, whose task prompt explains the source context and source-quality rule.

Read channels are explicit only:

- ✅ `:data {:filters {:channels ["..."]}}` — channels to scan/read.
- ✅ `:data {:filters {:guildIds ["..."]}}` — intentional guild-wide scan.
- ❌ `:publishChannels` as read context — publish channels are output sinks only.
- ❌ missing/empty `:channels` on a resource-sourced synthesis job — means no Discord read source, not "inherit global defaults".

Source quality is part of the boundary contract: ✅/`quality=good` source messages are preferred evidence; ❌/`quality=bad` source messages are forbidden and should be excluded by the adapter before rendering.

### Source resources

Runtime `:source` resources have two allowed roles:

- `:source/type :event-generator` — an actor-owned instance of a ClojureScript driver, such as Discord gateway events, Discord voice windows, or eta-mu session events.
- `:source/type :context` — a context provider hydrated before a turn.

Drivers are code, not resources. Event-generating sources declare a driver, an actor, and `:source/listens`, the driver event types this actor/account cares about. The actor owns credentials. The driver implementation owns emitted event shapes. Triggers still match events; they do not call source drivers directly.

```clojure
{:contract/kind :source
 :contract/id "discord_gateway"
 :source/id :source/discord-gateway
 :source/type :event-generator
 :source/driver :driver/discord
 :source/actor "discord_automation"
 :source/listens [:discord.message
                  :discord.message.mention]}
```

Context source resources are distinct from `:ingest_source` resources, which control indexing/discovery.

```clojure
;; Agent, actor, and role resources may declare sources.
:sources [:source/openplanner-memory
          {:source/ref :source/openplanner-memory
           :hydration {:mode :always :k 10}}]
```

Resolution order is deterministic: actor baseline sources, then role sources, then agent sources, then run/action-local overrides. Duplicate refs merge by canonical `:source/id`; later refs override earlier scalar settings while nested maps deep-merge.

The first context source is `:source/openplanner-memory` (`contracts/sources/openplanner_memory.edn`). It hydrates prior Knoxx session/action memory through the same OpenPlanner memory search path used by `memory_search`.

## Event/Trigger/Action/Schedule/Generator Resources

These resources must stay separate:

- **Generator**: event provenance; declares what can produce events.
- **Source**: actor-owned driver instance or context provider; event sources declare driver, actor, and `:source/listens`.
- **Schedule**: temporal rule; emits a synthetic event through dispatch.
- **Trigger**: listener actor agreement; observes event types and requests an action.
- **Action**: registered executable behavior; takes actor context and event data.
- **Agent**: executable prompt/model/capability spec; does not contain triggers,
  actions, schedules, generator declarations, or source/source-mode runtime fields.

Single event path:

```text
source/generator/schedule -> event -> trigger -> action
```

Do not put multiple resource maps in one EDN file. Do not place trigger maps,
schedule maps, or generator maps inside an action resource.

### Action resource example

```clojure
{:contract/kind :action
 :contract/id "hello-world"
 :action/id :actions/hello-world
 :action/kind :actions/hello-world
 :action/handler "knoxx.backend.domain.action.registry/run-action!"
 :action/responds-to [:message/greeting]
 :action/result :message/send.expectation}
```

### Trigger resource example

```clojure
{:contract/kind :trigger
 :contract/id "hello_world_inbox"
 :trigger/kind :event
 :trigger/listener "system_admin"
 :trigger/events [:message/greeting]
 :trigger/action :actions/hello-world}
```

### Schedule resource example

```clojure
{:contract/kind :schedule
 :contract/id "hello_world_morning_tick"
 :schedule/rule "*/30 * * * *"
 :schedule/generator "hello_world_demo"
 :schedule/event {:event/type :message/greeting
                  :event/payload {:name "world"}}}
```

### Generator resource example

```clojure
{:contract/kind :generator
 :contract/id "hello_world_demo"
 :generator/kind :demo
 :generator/actor "system_admin"
 :generator/emits [:message/greeting]}
```

### Context overflow policy

Agent resources may define a top-level `:context` clause to prevent unbounded session transcript growth:

```clojure
:context {:max-messages 40
          :max-chars 80000
          :preserve-system true}
```

Runtime behavior:

- The policy is applied when Knoxx rehydrates a session, before sending a prompt, and when persisting completed or failed turns.
- `:max-messages` keeps the newest N non-system messages when `:preserve-system` is true.
- `:max-chars` then keeps the newest messages whose approximate text/content-part size fits the budget, always retaining at least the newest body message.
- System messages are preserved by default so the agent resource remains active after pruning.
- This is deterministic sliding-window pruning, not summarization. Add summary memory separately if an agent must preserve older facts.

Legacy aliases accepted by the runtime are `:context-policy`, `:contextPolicy`, `:maxMessages`, `:max_chars`, and `:preserveSystem`, but new EDN resources should use the kebab-case `:context` form above.

### Common mistakes in agent resources

- `:agent {:role :contract_librarian}` — wrong. Use `:role/contract-librarian`.
- `:agent {:role :role/knowledge_worker}` — wrong. Slug is kebab: `:role/knowledge-worker`.
- Putting tools directly in `:agent` — wrong. Tools come from capabilities.
- Putting capabilities in `:agent {:capabilities [...]}` — wrong. They belong in `:actor {:capabilities [...]}`.

---

## Actor Resource (actors/<id>.edn)

{:actor/id            "contract_librarian"       ; snake_case, matches filename
 :actor/kind          :agent                      ; :agent | :human | :service
 :actor/org           "open-hax"
 :actor/label         "Contract Librarian"
 :actor/default-agent "contract_librarian"       ; agent resource id
 :actor/roles         [:role/contract-librarian]  ; kebab-case keyword with :role/ namespace
 :prompts {:system "..."}}

### Common mistakes in actor resources

- `:actor/roles [:role/contract_librarian]` — wrong. Must be `:role/contract-librarian`.
- Declaring tools here — do not. Tools come from roles via capabilities.

---

## Role Resource (roles/<slug>.edn)

Filename: `snake_case.edn`. The `:role/id` inside uses `:role/<kebab-slug>`.

{:role/id           :role/contract-librarian   ; kebab-case, :role/ namespace
 :role/capabilities [:cap/read
                     :cap/websearch
                     :cap/memory
                     :cap/semantic
                     :cap/contract-write
                     :cap/openplanner]
 ; Optional
 :role/permissions  ["agent.memory.read"]
 :prompts {:system "Role-scoped prompt added to every agent in this role."}}

### Common mistakes in role resources

- `:role/id :role/contract_librarian` — wrong. Must be `:role/contract-librarian`.
- Listing `:cap/tools` directly — wrong. Tools live in capability resources.

---

## Capability Resource (capabilities/cap_<slug>.edn)

Filename: `cap_<snake_slug>.edn`. The `:cap/id` uses `:cap/<kebab-slug>`.

{:cap/id    :cap/contract-write
 :cap/tools [:contract.list
             :contract.read
             :contract.validate
             :contract.write]
 :cap/user-surfaces
 [{:surface/id    :workspace/contracts-editor
   :surface/label "Contracts editor"
   :surface/kind  :editor
   :surface/routes ["/contracts"]
   :surface/description "Humans edit EDN here; agents use contract.list/read/validate/write."}]}

### Resolution chain for a capability ref

`:cap/read-directory`
  -> normalize-cap-id  -> `"cap/read-directory"`   (slash-separated string)
  -> cap-id->slug      -> `"cap_read_directory"`   (snake filename slug)
  -> capability-file-path -> `capabilities/cap_read_directory.edn`

---

## Policy Resource (policies/<id>.edn)

{:contract/id   "no_nrepl"
 :contract/kind :policy
 :contract/doc  "Prevents nREPL eval in production."
 :policy/denied ["nrepl.eval"]
 :policy/reasons {"nrepl.eval" "nREPL access is disabled in production."}}

Policy denial is absolute. It removes tools from the final granted set even if the actor
holds a role that grants them.

---

## The Resolution Chain

```
actor/roles        -> role resources    -> role/capabilities
                                        -> capability resources -> cap/tools -> [tool ids]

agent :actor/caps  -> capability resources (extra caps declared on the agent)
                                        -> cap/tools -> [tool ids]

All above merged   -> all-granted-tool-ids

policy resources   -> policy/denied     -> subtracted from granted
tool-call contract -> tools/allowed     -> final per-turn constraint

Result: ResolvedToolSuite { :tools :denied :denied-reasons }
```

---

## Checklist Before Writing a Resource

- filename stem matches `:contract/id` (agents/actions/triggers/schedules/generators/policies) or `:actor/id` / `:role/id` / `:cap/id`
- role refs use `:role/<kebab-slug>` everywhere — actor file, agent file, role file
- capability refs use `:cap/<kebab-slug>` everywhere
- `:agent {:role ...}` is `:role/<kebab-slug>`, not a bare snake keyword
- capabilities live in `:actor {:capabilities [...]}`, not in `:agent`
- tools are NOT declared in agent or role resources — only in capability resources
- trigger resources use `:trigger/kind :event`, `:trigger/events`, and `:trigger/action`
- schedule resources use `:schedule/rule` / `:schedule/cron` / `:schedule/at` plus `:schedule/event`; they never call actions directly
- generator resources use `:generator/kind` / `:generator/driver` and `:generator/emits`; new event provenance is `:event/generator`, not `:source-kind`
- source resources use `:source/driver`, `:source/actor`, and `:source/listens`; do not put event shapes in `:source/emits`
- action resources identify registered behavior with `:action/kind` and `:action/handler`; they do not contain trigger or schedule maps
- `:data` is static config; do not put mutable logs, timestamps, world_state, rosters, or invented `data/` folder paths there unless a real state backend consumes them
- `:contract/kind` is present and is a keyword while the loader migration keeps this discriminator
- EDN is valid — balanced brackets, no trailing commas, no JSON syntax
