---
title: "DoD Audit of Knoxx — Tables of Records"
category: contracts
created: 2026-04-19
original: 2026.04.19.15.18.39.md
status: note
---

## DoD Audit of Knoxx

### ToR — Tables of Records

What are the actual records in this system right now?

| Record | Current home | Problem |
|---|---|---|
| Contracts | Redis `contract:edn:<id>` + `contracts:index` set | EDN stored as a JSON string inside Redis. Double-serialized. |
| Event agent jobs | `runtime_config.cljs` `defn` returning maps | Not records at all — they're computed data from env vars |
| Roles + tool grants | `def role-tools` literal | Same — not addressable records, just a def |
| Actor identity | Implicit in auth session | No record type exists. Actors have no persistent EDN home |
| Receipts / turn events | `run_state.cljs`, broadcast via WS | Ephemeral, not durable |
| Session state | Redis + OpenPlanner | Two stores, no canonical schema |

**Decision:** The tables of records are: `actors/`, `contracts/`, `roles/`, `capabilities/`. All folders of EDN files on disk. Redis becomes a cache/index, not the source of truth. 

***

### D+I — Data + Interpreter

The biggest blob of branching logic that should be a data + interpreter pair is `compile-contract->sql` in `contracts_routes.cljs`.  It manually destructures contract fields into SQL shapes. That's an interpreter — but the dispatch table is implicit in its `let` bindings. Same for `validate-contract-edn`: it checks `:contract/id` and `:contract/kind` by hand instead of running a Malli schema.

**Decision:** The interpreter is the contract runtime. Its input is an EDN file. Its dispatch table is the clause `:kind` key on each clause in `:clauses`. The validator is Malli. Neither of these belong in `contracts_routes.cljs` — that file should only be the HTTP boundary.

The `event-agent-job->contract-edn` function is the most obvious D+I violation: it's a string-building function that manually constructs EDN.  That's the interpreter doing the data's job. **Drop it entirely** — agents write EDN files directly.

***

### PoT — Pipeline of Pure Transforms

The current request pipeline for saving a contract:

```
HTTP body (string)
  → validate-contract-edn   [parse + check fields]
  → handle-save-contract     [write to Redis]
  → redis/set-json           [re-serialize]
```

Two problems: the EDN is parsed, then immediately `pr-str`'d back to a string for storage. It also branches on Redis availability inside the handler. The pure pipeline should be:

```
raw-edn-text
  → parse-edn          ;; pure, throws on malformed
  → validate-schema    ;; pure, Malli
  → [ok? errors]       ;; branch happens HERE, once
  ;; if ok:
  → write-file!        ;; effect, disk
  → index-id!          ;; effect, Redis set-add only
```

The Redis store drops out as source of truth. Files are source of truth. Redis holds only the index set of known contract ids. 

***

### EL+P — Events + Projections

The receipt log (actor's append-only ledger) is already event-sourced conceptually — `run_state.cljs` appends run events and broadcasts them. But there's no projection layer: nothing reads the event log to answer questions like "what is the current state of this contract's fulfillment?" or "what has this actor done this session?"

**Decision:** Each session produces a `receipts.jsonl` append log. State is projected from it on demand. The `fulfillment-scores.jsonl` in the eta-mu spec is the right shape — generalize it to all receipt kinds.

***

### SaE — Schema at the Boundary

Right now `contracts_routes.cljs` validates by hand: 

```clojure
(not (string? id))    → error
(not (keyword? kind)) → error
```

That's it. No structural validation, no clause validation, no actor-id resolution check.

**Decision:** Malli schema at every HTTP boundary. One `parse-and-validate` fn that returns `{:ok bool :value parsed-map :errors [...]}`. Everything downstream receives the parsed map, never the string. The string only lives at the wire boundary.

***

### SaD — Strategy Table

`runtime_config.cljs` has role-based tool grants as a nested literal.  That's a strategy table hiding in a def. The strategy (which capabilities does role X get?) should be data:

```edn
;; roles/system_admin.edn
{:role/id          :role/system-admin
 :role/capabilities [:cap/read :cap/write :cap/bash
                     :cap/discord :cap/openplanner
                     :cap/email :cap/bluesky :cap/music]}
```

The interpreter reads the role file, resolves capability ids to tool lists, and injects them into the session. The branching in `normalize-event-agent-job` (170+ lines of merge logic) is the same pattern — it's a strategy table interpreter written as imperative code.

***

### SCFI — Sparse Components over Flat IDs

The current actor representation is implicit in the auth session: a user has a role string, and that role string is looked up in `role-tools`. There is no actor record. There is no actor id. There is no capability graph — just a flat string lookup.

**Decision:** Each actor is a file `actors/<id>.edn`:

```edn
{:actor/id     "riatzukiza"
 :actor/kind   :human
 :actor/org    "open-hax"
 :actor/roles  [:role/system-admin]
 ;; capabilities are derived from roles, not stored here
 ;; memory and receipt-log are runtime, not stored here
}
```

Capabilities are sparse components — an actor only carries the ids of their roles. The runtime resolves role → capabilities → tools at session start. This is exactly SCFI: flat id references, sparse composition, no god-objects.

***

### C/H — Commands + Pure Reducers

The `queue-agent-control!` fn in `agent_runtime.cljs` is an effect dispatcher. It handles two command kinds (`follow_up`, `steer`) imperatively.  It should be:

```edn
;; command
{:kind           :agent/steer
 :conversation-id "..."
 :session-id      "..."
 :run-id          "..."
 :message         "..."}
```

Dispatched through a pure reducer that validates the command shape, then passed to the single effect handler that calls `.steer session message`. The event emitted back is a receipt.

***

## The Restructuring Decision

**What stays in code:**
- HTTP route registration (thin boundary only)
- Malli validators (generated from schema EDN)
- File I/O effects (read/write EDN files)
- Redis as index cache only
- Session runtime (actor hydration, tool injection, pi session lifecycle)

**What moves to EDN files:**
- All actor definitions → `actors/<id>.edn`
- All role definitions → `roles/<id>.edn`
- All capability definitions → `capabilities/<id>.edn`
- All contracts → `contracts/<id>.edn`
- Runtime config defaults → `config.edn`
- Constitutional layer → `PRINCIPLE.edn`

**What gets deleted:**
- `event-agent-job->contract-edn` — agents write EDN, they don't need a converter
- `compile-contract->sql` — there is no SQL target; files are the store
- `contract-librarian-contract-edn` def — becomes `contracts/contract-librarian.edn` on disk
- `migrate-event-agents->contracts!` — one-time script, run once, then deleted
- `def role-tools` in `runtime_config.cljs` — replaced by role EDN files

**What the contract runtime does (pure interpreter loop):**

```
load-actor(id)           → actor-map        [read actors/<id>.edn]
resolve-roles(actor)     → [role-map ...]   [read roles/*.edn for actor's role ids]
resolve-caps(roles)      → [cap-map ...]    [read capabilities/*.edn]
resolve-tools(caps)      → [tool-id ...]    [flat list, deduplicated]
load-contract(id)        → contract-map     [read contracts/<id>.edn]
validate-clauses(c)      → ok | errors      [Malli]
build-session-ctx(actor, contract, tools) → session-ctx
run-triggers(ctx)        → fired? 
run-before-hooks(ctx)    → allow | block
dispatch-tool-call(ctx)  → result
run-after-hooks(result)  → verdict-record
append-receipt(verdict)  → receipts.jsonl
```

Every step is a pure transform except the three effects: file reads, tool dispatch, and receipt append. The branching logic collapses to the Malli validator and the clause dispatch table.
