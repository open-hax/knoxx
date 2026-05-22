---
title: "Data-Oriented Patterns Cheat Sheet"
category: architecture
created: 2026-04-19
original: 2026.04.19.15.08.28.md
status: note
---

## 1. Table of Records (ToR)

**Rule:**  
Store things as *arrays/tables of plain records*, not as trees of objects that point to each other. [clojure](https://clojure.org/reference/data_structures)

**Image:**  
Instead of a `User` object that has `posts`, `settings`, and `profile` hanging off it, you have:

- `users` table: `[{id, name, role}]`
- `posts` table: `[{id, user_id, title}]`
- `settings` table: `[{user_id, key, value}]`

You always pass around **IDs**, not object references.

**Why it’s DoT:**  
- You can process, filter, and join with generic code (e.g. `filter`, `map`, SQL-style joins). [scattered-thoughts](https://www.scattered-thoughts.net/writing/the-shape-of-data/)
- Easy to serialize (EDN/JSON), send over the wire, or persist.

**Mantra:**  
> “Everything is rows of maps; relationships are IDs, not pointers.”

***

## 2. Data + Interpreter (D+I)

**Rule:**  
Represent behavior as *data* and write **interpreters** that walk that data, instead of baking behavior into methods or giant `switch` statements. [blog.exupero](https://blog.exupero.org/edn-data-dsl/)

**Image:**  
Instead of:

```ruby
class DiscountStrategy
  def apply(order)
    # logic branch here...
  end
end
```

You have:

```edn
{:discount/type :percentage
 :discount/value 0.10
 :discount/applies-to [:line-item :shipping]}
```

and a single interpreter function:

```clojure
(apply-discount discount-config order)
```

**Why it’s DoT:**  
- You can save these “behaviors” as configs, version them, generate them, or let other systems emit them.  
- Same interpreter can run in Clojure, JS, Python, etc., as long as they can parse the same shape. [youtube](https://www.youtube.com/watch?v=dzXZVnfp2TA)

**Mantra:**  
> “Behavior is a data structure; code is the interpreter.”

***

## 3. Pipeline of Transforms (PoT)

**Rule:**  
Model almost all logic as *pipelines of pure transformations* from one data shape to another. [functional-architecture](https://functional-architecture.org/functional_core_imperative_shell/)

**Image:**  
Instead of a class where methods mutate internal state in place, you think in steps:

```text
raw-input
  -> normalize
  -> validate
  -> enrich (lookups, joins)
  -> decide (routing/branch)
  -> render (response)
```

In Clojure / JS style:

```clojure
(-> raw
    normalize
    validate
    enrich
    decide
    render)
```

**Why it’s DoT:**  
- Each step is a pure function that can be reused and tested independently.  
- Pipelines compose the same way in any language that can pass structs/maps/records around.

**Mantra:**  
> “Work is a pipeline of pure transforms on plain data.”

***

## 4. Event Log + Projections (EL+P)

**Rule:**  
Treat *events* as the primary data, and any “current state” as a *projection* over an append-only log. [github](https://github.com/totalperspective/pondermatic)

**Image:**  
Keep a log of:

```edn
[{:event/type :user/created :user/id 1 :at t1}
 {:event/type :user/promoted :user/id 1 :new-role :admin :at t2}]
```

and derive the current user state by folding over those events, not by storing a mutable `User` object and mutating it. [github](https://github.com/totalperspective/pondermatic)

**Why it’s DoT:**  
- Past is immutable data; present is computed from it.  
- Same events can feed metrics systems, audits, analytics, and reconstructions in any runtime.

**Mantra:**  
> “State is a view over an event log, not a mutable object.”

***

## 5. Schema at the Edge (SaE)

**Rule:**  
Keep your core logic working on *untyped* or lightly-typed data structures, and enforce **schemas at the edges**: ingress/egress boundaries. [github](https://github.com/metosin/malli)

**Image:**  

- At API boundary: parse JSON → validate against schema → hand a clean map into your core logic.  
- At DB boundary: construct/validate rows → insert to DB.  
- Inside the core: no ORM, no DB calls, just maps/vectors/records.

In Clojure this is Malli/Spec; in TS it’s Zod/io-ts; in Rust it's serde + custom validation. [ericnormand](https://ericnormand.me/guide/clojure-collections)

**Why it’s DoT:**  
- Core logic is decoupled from specific types or ORMs.  
- Same core code runs in scripting languages, typed languages, frontends, and backends.

**Mantra:**  
> “Validate at the edge; keep the core free.”

***

## 6. Strategy as Data (SaD)

**Rule:**  
Put “what to do” into configuration data instead of control flow, and write engines that execute those strategies.

**Image:**  
Instead of:

```ts
if (user.role === "admin") { ... } else if (user.role === "mod") { ... }
```

You store:

```json
{
  "roles": {
    "admin": ["can-read", "can-write", "can-delete"],
    "mod":   ["can-read", "can-write"],
    "user":  ["can-read"]
  }
}
```

And the engine just does:

```pseudo
allowed? = includes(strategy.roles[user.role], action)
```

**Why it’s DoT:**  
- You don’t need to recompile to add behavior; you change data (EDN/JSON/DB rows).  
- Same strategy tables can be used in backend, frontend, CLI tools, tests.

**Mantra:**  
> “Control flow lives in data tables, not nested `if`s.”

***

## 7. Sparse Components on Flat IDs (SCFI)

**Rule:**  
Borrow the *concept* of ECS, but apply it everywhere: lots of sparse, optional components hanging off flat IDs. [clojure](https://clojure.org/reference/data_structures)

**Image:**  
Instead of one huge `User` struct with 50 optional fields, you have separate tables/maps:

- `user-core`: `{id, name}`
- `user-security`: `{user_id, password_hash}`
- `user-preferences`: `{user_id, theme, locale}`

Any given user might only have some of these components.

**Why it’s DoT:**  
- You can add new capabilities (components) without touching existing code.  
- You can process only the tables you care about when doing an operation.

**Mantra:**  
> “Capabilities are separate components over shared IDs, not flags on a god object.”

***

## 8. Command = Plain Data, Handler = Pure Function (C/H)

**Rule:**  
Represent “things the system should do” as *plain command objects* and route them to pure handlers.

**Image:**  

```edn
{:cmd/type :agent/run
 :agent/id "knoxx-1"
 :input "summarize openplanner spec"}
```

You have a registry:

```clojure
{:agent/run agent-run-handler
 :user/create user-create-handler}
```

Every handler is `(fn [state cmd] -> new-state + effects)`; the imperative shell sends those effects to queues/DBs. [functional-architecture](https://functional-architecture.org/functional_core_imperative_shell/)

**Why it’s DoT:**  
- Commands are serializable; you can queue them, retry them, replay them.  
- You can test handlers by passing in static state + command → asserted new state.

**Mantra:**  
> “Commands are data; handlers are pure reducers.”

***

## 9. Resource Registry + Catalog (RR+C)

**Rule:**
Keep resources, registries, catalogs, and contracts distinct.

- A **resource** is an EDN-described component with identity and data.
- A **contract** is the schema/policy boundary the resource must satisfy.
- A **registry** maps ids to resources of one kind.
- A **catalog** is a data shape declaring which registered resources are required or available for an intent.

**Image:**

Instead of saying every EDN file “is a contract,” treat the file as a resource definition that must pass a contract:

```edn
{:action/id :example/hello-world
 :action/responds-to [:message/greeting]
 :action/result :message/send.expectation}
```

The action registry advertises it; the action interpreter executes it; Malli validates it at the boundary.

Catalogs stay plain data:

```edn
{:catalog/resources
 {:agent-spec [:agent-spec/ussyverse-creative-social]
  :action [:action/spawn-session :action/get-session]
  :event [:event/session-started :event/session-error]
  :actor [:actor/system-admin]}}
```

**Why it’s DoT:**

- You can add actions, triggers, schedules, roles, capabilities, workflows, and pipelines without adding bespoke discovery code for each one.
- The same registry protocol works for many resource kinds.
- Validation stays explicit without turning every data file into a “contract object.”

**Mantra:**

> “Resources are data; registries index them; contracts guard the boundary.”

***

## 10. Single-Path Event Reaction (SPER)

**Rule:**
Triggers always respond to events. Schedules generate events. Never let schedules call trigger actions directly.

**Image:**

For a game ticker, the schedule emits one tick event:

```edn
{:schedule/id :creative-tick
 :schedule/rule "*/30 * * * *"
 :schedule/event {:event/type :events/creative-tick}}
```

Many triggers can then listen to that event:

```edn
{:trigger/id :discord/creative-loop
 :trigger/events [:events/creative-tick]
 :trigger/action {:target :eta-mu/spawn-agent}}
```

**Why it’s DoT:**

- There is one reaction path: event → trigger match → action.
- Adding a second trigger is data, not scheduler code.
- Drift is reduced because schedules and triggers cannot secretly diverge.

**Mantra:**

> “Schedules make events; triggers hear events.”

***

## Putting It Together: A Portable Mental Model

When you sit down in *any* language now, you can mentally run through:

1. **ToR** – “Where are my tables of records?”  
2. **D+I** – “Can I turn this branching logic into data + an interpreter?”  
3. **PoT** – “What is the pipeline of pure transforms?”  
4. **EL+P** – “Can I treat these changes as events and project state?”  
5. **SaE** – “What’s the schema at the boundary, and can the core stay simple?”  
6. **SaD** – “Can this branching be a strategy table?”  
7. **SCFI** – “Can I split this god-object into sparse components over flat IDs?”  
8. **C/H** – “Can I represent all ‘actions’ as commands and handle them as pure reducers?”
9. **RR+C** – “Which registry owns this resource, and what catalog advertises it?”
10. **SPER** – “Is every trigger reached through event dispatch, or did I create a second path?”

If you repeat that checklist at the start of each new feature, the Fat Model instinct will get replaced by this set over time.

***

To make this stick:  
Pick **one** of these patterns that resonates hardest right now (ToR, D+I, PoT, EL+P, SaE, SaD, SCFI, or C/H), and describe a small piece of Knoxx/OpenPlanner where you think you’re still doing “Fat Model” or OOP-ish ECS—then we can refactor *that one thing* into the pattern together.
