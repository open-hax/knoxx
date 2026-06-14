# Spec: Knoxx Contract Runtime Deployment

**Epic:** `knoxx-contract-runtime-deployment`
**Date:** 2026-06-11
**Status:** done

## Summary

Knoxx is now a deployment of the contract runtime. Four phases completed:

1. **Runtime decomposition** — 17 core namespaces extracted to `@open-hax/contract-runtime` package
2. **Qualified-id resolution** — Role/cap/agent resolution preserves namespace-qualified ids
3. **Manifest migration** — 26 runtime-critical contract files migrated to 8 namespace manifests
4. **Anonymous facet adoption** — Grammar supports anonymous facets; interpreters read them in place

## Phase 1: Runtime Decomposition

**Package:** `packages/contract-runtime/`

17 namespaces extracted with dependency injection for three wiring namespaces:

| Namespace | Role |
|---|---|
| `manifest` | Manifest DSL parser |
| `store.law` | Store schema guard compiler |
| `store.protocol` | IStore protocol |
| `store.memory` | Process-local IStore backend |
| `store.registry` | Store instance resolution |
| `law.url` | URL detection predicates |
| `action.anonymous` | Safe evaluator for `:action/fn` |
| `action.interpreter` | Action execution engine |
| `driver.registry` | Driver protocol + registry |
| `condition.registry` | Condition registry + safe eval |
| `filter.registry` | Filter registry |
| `registry.resource` | Generic resource registry |
| `agent.context` | Thread-local turn context |
| `agent.reasoning` | Provider reasoning extraction |
| `agent.turn-guards` | Tool-call death-spiral detection |
| `agent.tool-lifecycle` | Tool lifecycle transforms |
| `agent.text-delta` | Stream text-delta helpers |

**Bridge:** `knoxx.backend.contract-runtime-deps/build-deps` provides injection map.

## Phase 2: Qualified-Id Resolution

Updated three functions to preserve namespace-qualified keywords:

- `roles/keywordish-id` — preserves `:deploy/greeter-role` → `"deploy/greeter-role"`
- `resolve/keywordish->role-slug` — preserves qualified role slugs
- `resolve/keywordish->capability-ref` — preserves qualified capability refs

Standard namespaces (`:role/`, `:cap/`, etc.) are still stripped. Non-standard namespaces (from namespace files) are preserved.

## Phase 3: Manifest Migration

8 namespace manifests created, 26 individual files deleted:

| Manifest | Resources |
|---|---|
| `discord.edn` | 2 sources, 1 trigger, 1 action, 1 trigger (voice audio) |
| `synthesis.edn` | 2 triggers, 2 actions |
| `patrol.edn` | 1 trigger, 1 action |
| `fork_tales.edn` | 1 schedule, 1 trigger |
| `ussyverse_social.edn` | 1 schedule, 1 trigger |
| `graphics.edn` | 1 trigger, 1 action |
| `core_sources.edn` | 8 sources |
| `knoxx_schedule.edn` | 1 generator |

## Phase 4: Anonymous Facet Adoption

The anonymous facet mechanism is demonstrated in `ussyverse.edn`:
- Trigger entry has `:action/scope` (anonymous action facet)
- Trigger entry has `:store/id` (registered store facet)
- Interpreters read these facets in place

The grammar supports anonymous facets for all 17 resource kinds. Full inline `:agent/*` facet support for `:actions/start-agent-session` is a follow-up task.

## Verification

- `shadow-cljs compile server` — 0 warnings
- `shadow-cljs compile test` — 0 failures, 0 warnings
