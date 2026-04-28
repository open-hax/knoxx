# Knoxx Contracts — Agent Authoring Guide

This document is the canonical reference for writing correct Knoxx contracts.
Read it before creating or editing any contract EDN file.

---

## Contract System Overview

Contracts are EDN files that declare the identity, capabilities, and constraints of every
actor in the Knoxx system. They live in `contracts/<class>/<id>.edn` and are loaded at
runtime by the contract loader.

The **filename stem** is the **contract id**. The loader derives the id from the filename;
`:contract/id` (or `:role/id`, `:cap/id`, etc.) inside the file must match.

---

## Directory Layout

```
contracts/
  agents/         Agent contracts — define a named AI persona with role, model, prompts, and tools
  actors/         Actor contracts — define a principal (human user or system agent)
  roles/          Role contracts — map a role slug to a set of capabilities
  capabilities/   Capability contracts — map a capability id to a set of tool ids
  policies/       Policy contracts — declare invariants, guardrails, and tool denials
  actions/        Action contracts — declarative event-driven task specs
  pipelines/      Pipeline contracts — ordered action sequences
  triggers/       Trigger contracts — schedule or event conditions that fire pipelines
  models/         Model contracts — individual model metadata
  model_families/ Model family contracts — shared properties across model variants
  ensemble/       Grouped agent contracts for multi-agent sessions
```

---

## ID and Namespace Conventions

Contract ids follow a namespaced convention that mirrors the filesystem.

| Contract class | Id form | Example |
|---|---|---|
| agents | plain string, snake_case | `"my_agent"` |
| actors | plain string, snake_case | `"chat_primary"` |
| roles | keyword `:role/<kebab-slug>` | `:role/contract-librarian` |
| capabilities | keyword `:cap/<kebab-slug>` | `:cap/contract-write` |
| policies | plain string, snake_case | `"no_nrepl"` |
| models | plain string, snake_case | `"glm_5"` |

**Namespace is allowed and expected on role and capability ids.**
`:role/contract-librarian` normalises to slug `contract-librarian` which maps to file `roles/contract_librarian.edn`.
`:cap/read-directory` normalises to slug `cap_read_directory` which maps to `capabilities/cap_read_directory.edn`.

File names always use `snake_case`. Keywords inside contracts use `kebab-case`.
The runtime bridges both forms. **Never mix them within the same contract.**

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

## Agent Contract (agents/<id>.edn)

{:contract/id    "my_agent"         ; must match filename stem
 :contract/kind  :agent
 :contract/version 1
 :enabled        true
 :trigger-kind   :manual            ; :manual | :event | :scheduled

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
 :data    {}                        ; arbitrary runtime config
 :hooks   {:before {} :after {}}}

Roles are **composable**. Use `:role` for a single role or `:roles` for a vector of roles. Both feed into `agent-role-claims` which merges them. Tools and system prompts are composed across ALL declared roles.

### Common mistakes in agent contracts

- `:agent {:role :contract_librarian}` — wrong. Use `:role/contract-librarian`.
- `:agent {:role :role/knowledge_worker}` — wrong. Slug is kebab: `:role/knowledge-worker`.
- Putting tools directly in `:agent` — wrong. Tools come from capabilities.
- Putting capabilities in `:agent {:capabilities [...]}` — wrong. They belong in `:actor {:capabilities [...]}`.

---

## Actor Contract (actors/<id>.edn)

{:actor/id            "contract_librarian"       ; snake_case, matches filename
 :actor/kind          :agent                      ; :agent | :human | :service
 :actor/org           "open-hax"
 :actor/label         "Contract Librarian"
 :actor/default-agent "contract_librarian"       ; agent contract id
 :actor/roles         [:role/contract-librarian]  ; kebab-case keyword with :role/ namespace
 :prompts {:system "..."}}

### Common mistakes in actor contracts

- `:actor/roles [:role/contract_librarian]` — wrong. Must be `:role/contract-librarian`.
- Declaring tools here — do not. Tools come from roles via capabilities.

---

## Role Contract (roles/<slug>.edn)

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

### Common mistakes in role contracts

- `:role/id :role/contract_librarian` — wrong. Must be `:role/contract-librarian`.
- Listing `:cap/tools` directly — wrong. Tools live in capability contracts.

---

## Capability Contract (capabilities/cap_<slug>.edn)

Filename: `cap_<snake_slug>.edn`. The `:cap/id` uses `:cap/<kebab-slug>`.

{:cap/id    :cap/contract-write
 :cap/tools [:contract.write]
 :cap/user-surfaces
 [{:surface/id    :workspace/contracts-editor
   :surface/label "Contracts editor"
   :surface/kind  :editor
   :surface/routes ["/contracts"]
   :surface/description "Humans edit EDN here; agents use contract.write."}]}

### Resolution chain for a capability ref

`:cap/read-directory`
  -> normalize-cap-id  -> `"cap/read-directory"`   (slash-separated string)
  -> cap-id->slug      -> `"cap_read_directory"`   (snake filename slug)
  -> capability-file-path -> `capabilities/cap_read_directory.edn`

---

## Policy Contract (policies/<id>.edn)

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
actor/roles        -> role contracts    -> role/capabilities
                                        -> capability contracts -> cap/tools -> [tool ids]

agent :actor/caps  -> capability contracts (extra caps declared on the agent)
                                        -> cap/tools -> [tool ids]

All above merged   -> all-granted-tool-ids

policy/contracts   -> policy/denied     -> subtracted from granted
tool-call contract -> tools/allowed     -> final per-turn constraint

Result: ResolvedToolSuite { :tools :denied :denied-reasons }
```

---

## Checklist Before Writing a Contract

- filename stem matches `:contract/id` (agents/actors/policies) or `:role/id` / `:cap/id`
- role refs use `:role/<kebab-slug>` everywhere — actor file, agent file, role file
- capability refs use `:cap/<kebab-slug>` everywhere
- `:agent {:role ...}` is `:role/<kebab-slug>`, not a bare snake keyword
- capabilities live in `:actor {:capabilities [...]}`, not in `:agent`
- tools are NOT declared in agent or role contracts — only in capability contracts
- `:contract/kind` is present and is a keyword
- EDN is valid — balanced brackets, no trailing commas, no JSON syntax