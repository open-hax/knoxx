---
title: "Docs/Notes File Audit — Flat List Analysis"
category: contracts
created: 2026-04-17
original: 2026.04.17.10.11.17.md
status: note
---

Now I have the full picture. 36 files, all flat. Here's the analysis and the proposal:

## What the flat list actually contains

Grouping by what the files *do*, not what they're named:

| Actual role | Current files |
|---|---|
| **HTTP route handlers** | `admin_routes`, `app_routes`, `document_routes`, `memory_routes`, `model_routes`, `multimodal_routes`, `tool_routes`, `translation_routes`, `voice_routes` |
| **Agent runtime core** | `agent_runtime`, `agent_turns`, `agent_hydration`, `agent_templates`, `run_state`, `turn_control` |
| **Contract/config layer** | `event_agents`, `runtime_config`, `policy_db` |
| **Platform integrations** | `discord_cron`, `discord_gateway`, `mcp_bridge`, `pi_session_ingester` |
| **Session/auth** | `auth_session`, `authz`, `session_store`, `session_recovery`, `session_titles` |
| **Infrastructure** | `core`, `http`, `realtime`, `redis_client` |
| **Data/domain** | `app_shapes`, `document_state`, `openplanner_memory`, `core_memory`, `text`, `tooling` |

The big problems: `event_agents.cljs` (36KB) and `runtime_config.cljs` (37KB) are doing contract loading, agent config resolution, tool policy, trigger dispatch, and DB writes all in one file each. `agent_turns.cljs` at 53KB is the entire turn execution pipeline in one namespace. These need to be split along the seams, not just reorganized into folders.

***

## Proposed tree

```
knoxx/backend/
├── core.cljs                     ;; unchanged — entry, mount, env
├── http.cljs                     ;; unchanged — server bootstrap
├── realtime.cljs                 ;; unchanged — ws/sse broadcast
├── redis_client.cljs             ;; unchanged
│
├── routes/
│   ├── admin.cljs                ;; ← admin_routes
│   ├── app.cljs                  ;; ← app_routes
│   ├── documents.cljs            ;; ← document_routes
│   ├── memory.cljs               ;; ← memory_routes
│   ├── models.cljs               ;; ← model_routes
│   ├── multimodal.cljs           ;; ← multimodal_routes
│   ├── tools.cljs                ;; ← tool_routes (HTTP side only)
│   ├── translations.cljs         ;; ← translation_routes
│   └── voice.cljs                ;; ← voice_routes
│
├── contract/
│   ├── schema.cljs               ;; NEW — malli schemas (from last session)
│   ├── bracket.cljs              ;; NEW — repair/diagnose
│   ├── sci.cljs                  ;; NEW — sci whitelist + eval ctx
│   ├── loader.cljs               ;; extracted from runtime_config — edn->AST->SQL
│   ├── field_registry.cljs       ;; NEW — widget/type metadata for admin UI
│   ├── compiler.cljs             ;; extracted from runtime_config — AST->SQL projections
│   └── agents/
│       ├── discord_patrol.edn    ;; native contracts live here as data
│       ├── github_monitor.edn
│       └── ...
│
├── runtime/
│   ├── agent.cljs                ;; extracted core of agent_runtime — start/stop/dispatch
│   ├── turns.cljs                ;; ← agent_turns — turn loop, tool calls, streaming
│   ├── hydration.cljs            ;; ← agent_hydration — prompt/context assembly
│   ├── templates.cljs            ;; ← agent_templates
│   ├── run_state.cljs            ;; ← run_state — in-flight run tracking
│   ├── turn_control.cljs         ;; ← turn_control
│   └── hooks.cljs                ;; NEW — :before/:after hook dispatch (from contract)
│
├── tools/
│   ├── registry.cljs             ;; extracted from tooling — tool def + lookup
│   ├── dispatch.cljs             ;; extracted from tooling/tool_routes — call routing
│   ├── policies.cljs             ;; extracted from runtime_config/policy_db — tool policy eval
│   └── impl/
│       ├── memory.cljs           ;; tool implementations, one file per domain
│       ├── documents.cljs
│       ├── search.cljs
│       └── ...
│
├── triggers/
│   ├── cron.cljs                 ;; extracted from discord_cron + runtime_config
│   ├── event.cljs                ;; extracted from event_agents — event-kind dispatch
│   └── gateway.cljs              ;; ← discord_gateway (renamed, source-agnostic interface)
│
├── integrations/
│   ├── discord/
│   │   ├── client.cljs           ;; discord API calls
│   │   ├── gateway.cljs          ;; ws connection, heartbeat
│   │   └── events.cljs           ;; raw discord event -> normalized event-kind
│   ├── mcp/
│   │   └── bridge.cljs           ;; ← mcp_bridge
│   └── pi/
│       └── ingester.cljs         ;; ← pi_session_ingester
│
├── session/
│   ├── auth.cljs                 ;; ← auth_session
│   ├── authz.cljs                ;; ← authz
│   ├── store.cljs                ;; ← session_store
│   ├── recovery.cljs             ;; ← session_recovery
│   └── titles.cljs               ;; ← session_titles
│
├── memory/
│   ├── core.cljs                 ;; ← core_memory
│   └── openplanner.cljs          ;; ← openplanner_memory
│
├── models/
│   └── shapes.cljs               ;; ← app_shapes (domain data shapes)
│
└── util/
    └── text.cljs                 ;; ← text — string/chunking utilities
```

***

## The critical splits inside the big files

**`runtime_config.cljs` (37KB) splits into:**
- `contract/loader.cljs` — reads EDN, runs bracket repair, schema-validates, returns `{:ok :contract :errors}`
- `contract/compiler.cljs` — takes validated AST, writes SQL projections
- `tools/policies.cljs` — tool policy evaluation rules
- `runtime/agent.cljs` — the `start-agent-run!` entry point that consumes a loaded contract

**`event_agents.cljs` (36KB) splits into:**
- `triggers/event.cljs` — event-kind routing table, which contracts subscribe to what
- `contract/loader.cljs` — the load/validate/store cycle (shared with above)
- `runtime/hooks.cljs` — `:before`/`:after` hook invocation

**`agent_turns.cljs` (53KB) stays mostly intact** but the tool-call dispatch inner loop moves to `tools/dispatch.cljs` so the turn file owns the *loop structure* and tools own *what happens inside a call*.

**`policy_db.cljs` (69KB)** — this is the biggest one. It likely contains both schema/migrations and query functions mixed together. It needs:
- Query functions stay in `contract/compiler.cljs` or `tools/policies.cljs` depending on what they query
- Any raw SQL schema strings move to a `db/migrations/` folder outside `src/`

***

## Namespace convention

```clojure
;; routes — thin HTTP adapters, no business logic
knoxx.backend.routes.admin
knoxx.backend.routes.tools        ;; HTTP side of tool calls

;; contract — the EDN DSL pipeline
knoxx.backend.contract.schema
knoxx.backend.contract.bracket
knoxx.backend.contract.sci
knoxx.backend.contract.loader
knoxx.backend.contract.compiler
knoxx.backend.contract.field-registry

;; runtime — agent execution
knoxx.backend.runtime.agent
knoxx.backend.runtime.turns
knoxx.backend.runtime.hydration
knoxx.backend.runtime.hooks
knoxx.backend.runtime.run-state

;; tools — callable tool surface
knoxx.backend.tools.registry
knoxx.backend.tools.dispatch
knoxx.backend.tools.policies
knoxx.backend.tools.impl.memory

;; triggers — what wakes an agent
knoxx.backend.triggers.cron
knoxx.backend.triggers.event

;; integrations — external services
knoxx.backend.integrations.discord.gateway
knoxx.backend.integrations.discord.events
knoxx.backend.integrations.mcp.bridge
```

***

## Dependency ordering (innermost → outermost, no cycles)

```
util/text
  ↓
models/shapes
  ↓
contract/schema  contract/bracket  contract/sci
  ↓                    ↓                ↓
contract/loader ←──────────────────────┘
  ↓
contract/compiler    tools/registry    session/*
  ↓                      ↓
tools/policies       tools/impl/*
  ↓                      ↓
tools/dispatch ←─────────┘
  ↓
runtime/hydration  runtime/templates  runtime/run-state  runtime/hooks
  ↓                     ↓                   ↓                  ↓
runtime/turns ←──────────────────────────────────────────────┘
  ↓
runtime/agent       triggers/*        integrations/*
  ↓                     ↓                  ↓
routes/* ←───────────────────────────────────┘
```

No file in `contract/` should import from `runtime/` or `routes/`. No file in `runtime/` should import from `routes/`. The arrows only go up.

***

## Countermove on `policy_db`

At 69KB that file is almost certainly doing DB schema, migrations, CRUD queries, and business logic all together. Before reorganizing it, worth a quick skim to see if there's a hidden `db/` layer that should become its own subtree:

```
db/
├── migrations.cljs    ;; table definitions, SQL strings
├── queries.cljs       ;; raw honey-sql / pg query fns
└── pool.cljs          ;; connection pool setup
```

If `policy_db` is genuinely just policy rules that happen to hit the DB, it can stay as `tools/policies.cljs`. If it's also the only place migrations live, that needs to come out first before anything else moves.

***

## Next

The safest migration order:

1. Create the folder skeleton with empty namespace stubs (no logic moved yet) — this gives you the `require` paths to start wiring
2. Move `util/`, `models/`, `session/` first — zero deps on the hard stuff
3. Split `contract/` out of `runtime_config` and `event_agents` — this unlocks everything downstream
4. Move `runtime/` once `contract/` is stable
5. Move `tools/` — depends on both `contract/` and `runtime/`
6. Move `routes/` last — they're the thinnest layer and depend on everything

Want me to generate the stub namespace files as a commit to a branch, or write out the `deps.edn` / shadow-cljs `:source-paths` changes first?
