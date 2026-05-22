# Contract Runtime: Data-Oriented Restructure

**Date:** 2026-04-19  
**Status:** Accepted  
**Supersedes:** `docs/notes/2026.04.17.10.11.17.md`

---

## Framing: Why "Actor"

The system needs a single term for any decision-making entity — human or AI. "User" is human-only by convention. "Agent" is AI-only by convention. "Actor" is semantically neutral: it denotes any entity that has agency and takes actions in the system. The term appears in the actor model of computation, in theatre (a role that acts), and in legal contexts (a party who acts). None of these connotations conflict with the intended meaning here.

- `:actor/kind :human` — a person operating through the UI or API
- `:actor/kind :ai` — an AI agent operating under a contract
- All actors have an id, roles, and capabilities. No other distinction at the data layer.

---

## What's Wrong with the Previous Plan (`2026.04.17.10.11.17.md`)

The prior plan correctly identified the namespace structure and dependency ordering. It fails on three points:

**1. `contract/` is a code namespace masquerading as a data namespace.**  
The plan puts `contract/loader.cljs`, `contract/compiler.cljs`, `contract/schema.cljs` under a `contract/` folder alongside `contract/agents/*.edn`. This conflates the data (EDN files) with the interpreter (ClojureScript). The interpreter belongs in `runtime/`. The data belongs in `contracts/` (a pure data directory, no `.cljs` files).

**2. `policy_db.cljs` gets punted.**  
The plan hedges with "worth a quick skim." It's not ambiguous: capability grants (`role-tools`, tool policy lists) are strategy tables that belong as EDN files, not as code. `policy_db.cljs` must be dissolved — policy data → EDN, DB queries → `db/queries.cljs`, migrations → `db/migrations/`.

**3. `contract/compiler.cljs` shouldn't exist.**  
`compile-contract->sql` is dead code: there is no SQL target. Files are source of truth. Redis holds only the index set of known contract ids. The "compiler" is just a Malli validator. Name it accordingly: `runtime/contract_validator.cljs`.

---

## The Corrected Structure

### Data Layer (no `.cljs` files)

```
contracts/
├── actors/
│   └── <actor-id>.edn        ;; one file per actor (human or AI)
├── roles/
│   ├── system_admin.edn
│   ├── developer.edn
│   ├── knowledge_worker.edn
│   └── ...
├── capabilities/
│   ├── cap_read.edn
│   ├── cap_write.edn
│   ├── cap_bash.edn
│   └── ...
└── agents/
    ├── discord_patrol.edn
    ├── contract_librarian.edn  ;; replaces the hardcoded def
    └── ...
```

**Schema for each record type:**

```edn
;; actors/<id>.edn
{:actor/id    "riatzukiza"
 :actor/kind  :human
 :actor/org   "open-hax"
 :actor/roles [:role/system-admin]}

;; roles/system_admin.edn
{:role/id           :role/system-admin
 :role/capabilities [:cap/read :cap/write :cap/bash
                     :cap/discord :cap/openplanner
                     :cap/email :cap/bluesky :cap/music]}

;; capabilities/cap_read.edn
{:cap/id    :cap/read
 :cap/tools [:read :websearch :memory_search :graph_query]}

;; contracts/agents/discord_patrol.edn
{:contract/id      "discord_patrol"
 :contract/kind    :agent
 :contract/version 1
 :enabled          true
 :actor/roles      [:role/knowledge-worker]
 :trigger          {:kind :event :event-kinds [:discord/message]}
 :prompts          {:system "..." :task "..."}
 :data             {:filters {:channels [] :keywords []}}}
```

### Code Layer (interpreters only)

```
backend/src/cljs/knoxx/backend/
│
├── core.cljs                        ;; entry, mount, env — unchanged
├── http.cljs                        ;; server bootstrap — unchanged
├── realtime.cljs                    ;; ws/sse broadcast — unchanged
├── redis_client.cljs                ;; unchanged
│
├── db/                              ;; extracted from policy_db.cljs
│   ├── pool.cljs                    ;; connection pool
│   ├── migrations.cljs              ;; schema strings only
│   └── queries.cljs                 ;; honeysql query fns
│
├── runtime/
│   ├── config.cljs                  ;; env vars only — slimmed runtime_config
│   ├── contract_loader.cljs         ;; read EDN file → parsed map
│   ├── contract_validator.cljs      ;; Malli validate → {:ok :errors :value}
│   ├── sci_eval.cljs                ;; SCI whitelist + eval ctx for :expr clauses
│   ├── agent.cljs                   ;; session lifecycle (start/stop/dispatch)
│   ├── turns.cljs                   ;; turn loop — from agent_turns
│   ├── hydration.cljs               ;; context assembly — from agent_hydration
│   ├── templates.cljs               ;; from agent_templates
│   ├── run_state.cljs               ;; in-flight run tracking
│   ├── turn_control.cljs
│   └── hooks.cljs                   ;; :before/:after clause dispatch
│
├── tools/
│   ├── registry.cljs                ;; tool def + lookup
│   ├── dispatch.cljs                ;; call routing (inner loop from agent_turns)
│   └── impl/
│       ├── memory.cljs
│       ├── documents.cljs
│       ├── search.cljs
│       └── ...
│
├── triggers/
│   ├── cron.cljs                    ;; from discord_cron + runtime_config
│   └── event.cljs                   ;; from event_agents — event-kind dispatch
│
├── integrations/
│   ├── discord/
│   │   ├── client.cljs
│   │   ├── gateway.cljs
│   │   └── events.cljs              ;; raw discord event → normalized event-kind
│   ├── mcp/
│   │   └── bridge.cljs
│   └── pi/
│       └── ingester.cljs
│
├── session/
│   ├── auth.cljs
│   ├── authz.cljs
│   ├── store.cljs
│   ├── recovery.cljs
│   └── titles.cljs
│
├── memory/
│   ├── core.cljs
│   └── openplanner.cljs
│
├── routes/                          ;; thin HTTP adapters, no business logic
│   ├── admin.cljs
│   ├── app.cljs
│   ├── documents.cljs
│   ├── memory.cljs
│   ├── models.cljs
│   ├── multimodal.cljs
│   ├── tools.cljs
│   ├── translations.cljs
│   └── voice.cljs
│
└── util/
    ├── text.cljs
    └── bracket.cljs                 ;; EDN bracket repair
```

---

## Dependency Ordering (no cycles, arrows go up only)

```
util/text  util/bracket
     ↓          ↓
runtime/contract_loader
     ↓
runtime/contract_validator   tools/registry   session/*
     ↓                            ↓
runtime/hydration   runtime/templates   runtime/run_state   runtime/hooks
     ↓                    ↓                   ↓                   ↓
runtime/turns ←────────────────────────────────────────────────────┘
     ↓
runtime/agent       triggers/*        integrations/*      db/*
     ↓                  ↓                   ↓
routes/* ←──────────────────────────────────┘
```

No file in `runtime/` imports from `routes/`. No file in `tools/` imports from `runtime/`. The DB layer (`db/`) has no imports from any of the above.

---

## The Contract Runtime Interpreter Loop

Every step is a pure transform except the three labeled effects.

```
load-actor(id)
  → read contracts/actors/<id>.edn          [effect: file read]
  → actor-map

resolve-roles(actor-map)
  → read contracts/roles/<role-id>.edn      [effect: file read, per role]
  → [role-map ...]

resolve-capabilities(role-maps)
  → read contracts/capabilities/<cap-id>.edn [effect: file read, per cap]
  → [cap-map ...]

resolve-tools(cap-maps)
  → flat deduplicated [tool-id ...]         [pure]

load-contract(id)
  → read contracts/agents/<id>.edn          [effect: file read]
  → raw-edn-string

parse-edn(raw-edn-string)
  → parsed-map | parse-error               [pure]

validate-schema(parsed-map)
  → {:ok bool :value map :errors [...]}    [pure, Malli]

build-session-ctx(actor, contract, tools)
  → session-ctx                             [pure]

run-triggers(session-ctx)
  → fired? bool                             [pure dispatch, effect in trigger impl]

run-before-hooks(session-ctx)
  → :allow | :block                         [pure]

dispatch-tool-call(session-ctx, tool-call)
  → result                                  [effect: tool impl]

run-after-hooks(session-ctx, result)
  → verdict-record                          [pure]

append-receipt(verdict-record)
  → receipts.jsonl                          [effect: append]
```

---

## What Gets Deleted

| Item | Reason |
|---|---|
| `event-agent-job->contract-edn` fn | Agents write EDN directly. No converter needed. |
| `compile-contract->sql` fn | No SQL target. Files are source of truth. |
| `contract-librarian-contract-edn` def | Becomes `contracts/agents/contract_librarian.edn` |
| `migrate-event-agents->contracts!` fn | One-time migration. Run once, then delete. |
| `def role-tools` in `runtime_config` | Replaced by `contracts/roles/*.edn` |
| Inline tool policy lists | Replaced by `contracts/capabilities/*.edn` |
| `normalize-event-agent-job` (~170 lines) | Replaced by EDN files + loader |

---

## What `runtime_config.cljs` Becomes

After the split, `runtime/config.cljs` reads env vars and returns a plain map. No roles, no tool lists, no job definitions. Target: under 60 lines.

```clojure
(ns knoxx.backend.runtime.config)

(defn cfg []
  {:redis-url    (js/process.env.REDIS_URL)
   :db-url       (js/process.env.DATABASE_URL)
   :port         (js/parseInt (or js/process.env.PORT "3000"))
   :env          (or js/process.env.NODE_ENV "development")
   :contracts-dir (or js/process.env.CONTRACTS_DIR "contracts")})
```

Everything else comes from EDN files, resolved at session start by the loader.

---

## Migration Order

Execute in this sequence. Do not move namespaces before extracting data.

1. **Extract `db/`** from `policy_db.cljs` — unblocks everything downstream
2. **Write `contracts/` EDN files** — roles, capabilities, actors, agent jobs
3. **Write `runtime/contract_loader.cljs`** — reads + parses EDN from disk
4. **Write `runtime/contract_validator.cljs`** — Malli schema validation
5. **Slim `runtime_config.cljs` → `runtime/config.cljs`** — env vars only, delete all inlined data
6. **Split `event_agents.cljs`** → `triggers/event.cljs` + `triggers/cron.cljs`
7. **Move `util/`, `session/`, `memory/`** — zero risk, no hard dependencies
8. **Split `agent_turns.cljs`** — tool dispatch inner loop → `tools/dispatch.cljs`
9. **Move `routes/`** last — thinnest layer, depends on everything

Moving namespaces before step 5 produces god-objects in new directories. The data must be extracted first.

---

## Redis Role After Migration

| Before | After |
|---|---|
| Source of truth for contracts | Cache + index only |
| Stores full EDN text as JSON string | Stores only the set of known contract ids |
| `contract:edn:<id>` keys | Deleted |
| `contracts:index` set | Kept — used for fast listing |

Redis is write-through: when a contract EDN file is written to disk, the id is added to the index set. When a file is deleted, the id is removed. The file is always canonical.
